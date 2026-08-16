package com.project.helpcircle.presentation.help

import com.project.helpcircle.domain.engine.ForegroundAppTracker
import com.project.helpcircle.domain.model.ChargeWallet
import com.project.helpcircle.domain.model.CommunityMember
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.model.UserIdentity
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.NudgeRepository
import com.project.helpcircle.domain.repository.UserRepository
import com.project.helpcircle.domain.usecase.ConsumeChargeUseCase
import com.project.helpcircle.domain.usecase.IsFocusModeActiveUseCase
import com.project.helpcircle.domain.usecase.ObserveChargeWalletUseCase
import com.project.helpcircle.domain.usecase.ObserveCommunityStateUseCase
import com.project.helpcircle.domain.usecase.ObserveHelpablePeersUseCase
import com.project.helpcircle.domain.usecase.SendNudgeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Far enough in the future that no test's wall-clock read ever triggers passive replenishment mid-assertion. */
private const val FROZEN_LAST_REPLENISHED_AT = Long.MAX_VALUE / 2

/** The anonymous hash [HelpFakeUserRepository] reports, i.e. the roster entry that must never be offered as a target. */
private const val SELF_ID = "uid"

private class HelpFakeCommunityRepository(
    private val activeCommunityId: String?,
    private val communityStateFlow: MutableStateFlow<CommunityState> =
        MutableStateFlow(CommunityState("comm-1", emptyList(), cohesionBonusApplied = false)),
    private val memberCount: Int = 0
) : CommunityRepository {
    override fun observeCommunityState(communityId: String): Flow<CommunityState> = communityStateFlow
    override suspend fun joinCommunity(communityId: String): CommunityState = throw UnsupportedOperationException()
    override suspend fun createCommunity(communityId: String, inviteCode: String): CommunityState = throw UnsupportedOperationException()
    override suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState? = null
    override suspend fun reportCrisis(communityId: String) = Unit
    override suspend fun reportRecovery(communityId: String) = Unit
    override suspend fun publishSatisfaction(
        communityId: String,
        weekStartEpochMillis: Long,
        satisfaction: WeeklySatisfaction
    ) = Unit
    override suspend fun leaveCommunity(communityId: String) = Unit
    override suspend fun getActiveCommunityId(): String? = activeCommunityId
    override suspend fun getMemberCount(communityId: String): Int = memberCount
}

private class HelpFakeUserRepository(currentCharges: Int) : UserRepository {
    val walletFlow = MutableStateFlow(
        ChargeWallet(currentCharges = currentCharges, lastReplenishedAtEpochMillis = FROZEN_LAST_REPLENISHED_AT)
    )
    override suspend fun getOrCreateIdentity(): UserIdentity = UserIdentity(anonymousHash = SELF_ID, nickname = "nick")
    override suspend fun saveNickname(nickname: String) = Unit
    override val chargeWallet: Flow<ChargeWallet> = walletFlow
    override suspend fun updateChargeWallet(wallet: ChargeWallet) {
        walletFlow.value = wallet
    }
}

private class HelpFakeNudgeRepository : NudgeRepository {
    val incomingFlow = MutableSharedFlow<Nudge>(extraBufferCapacity = 1)
    var lastSentTarget: String? = null
    var lastSentNudge: Nudge? = null

    override val incomingNudges: Flow<Nudge> = incomingFlow
    override suspend fun sendNudge(communityId: String, targetUserId: String, nudge: Nudge) {
        lastSentTarget = targetUserId
        lastSentNudge = nudge
    }
}

private fun member(id: String, status: MemberStatus = MemberStatus.OK) =
    CommunityMember(id, "nick-$id", status, agencyScore = 50)

private fun rosterOf(vararg members: CommunityMember) =
    MutableStateFlow(CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, members = members.toList()))

@OptIn(ExperimentalCoroutinesApi::class)
class HelpViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        communityRepository: CommunityRepository,
        userRepository: UserRepository = HelpFakeUserRepository(currentCharges = 5),
        nudgeRepository: NudgeRepository = HelpFakeNudgeRepository()
    ): HelpViewModel {
        val observeChargeWallet =
            ObserveChargeWalletUseCase(userRepository, IsFocusModeActiveUseCase(ForegroundAppTracker()))
        return HelpViewModel(
            communityRepository,
            observeChargeWallet,
            ObserveHelpablePeersUseCase(ObserveCommunityStateUseCase(communityRepository), userRepository),
            SendNudgeUseCase(nudgeRepository, communityRepository, ConsumeChargeUseCase(userRepository, observeChargeWallet))
        )
    }

    @Test
    fun `no active community shows the no-circle state`() {
        val viewModel = viewModel(HelpFakeCommunityRepository(activeCommunityId = null))

        val state = viewModel.uiState.value
        assertEquals(false, state.hasActiveCommunity)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `only peers at risk or in crisis are offered as targets`() {
        val roster = rosterOf(
            member(SELF_ID, MemberStatus.CRISIS),
            member("calm", MemberStatus.OK),
            member("slipping", MemberStatus.AT_RISK),
            member("struggling", MemberStatus.CRISIS)
        )
        val viewModel = viewModel(HelpFakeCommunityRepository("comm-1", roster))

        val state = viewModel.uiState.value
        assertEquals(listOf("struggling", "slipping"), state.peersNeedingHelp.map { it.anonymousId })
        // Every peer counts toward the total, so the screen can tell "no peers" from "nobody needs help".
        assertEquals(3, state.totalPeerCount)
    }

    @Test
    fun `a circle where everyone is fine offers nobody but still reports its peers`() {
        val roster = rosterOf(member(SELF_ID), member("calm-one"), member("calm-two"))
        val viewModel = viewModel(HelpFakeCommunityRepository("comm-1", roster))

        val state = viewModel.uiState.value
        assertTrue(state.peersNeedingHelp.isEmpty())
        assertEquals(2, state.totalPeerCount)
    }

    @Test
    fun `a solo circle has no peers at all`() {
        val roster = rosterOf(member(SELF_ID, MemberStatus.CRISIS))
        val viewModel = viewModel(HelpFakeCommunityRepository("comm-1", roster))

        val state = viewModel.uiState.value
        assertTrue(state.peersNeedingHelp.isEmpty())
        assertEquals(0, state.totalPeerCount)
    }

    @Test
    fun `the charge wallet's current balance is reflected in the ui state`() {
        val userRepository = HelpFakeUserRepository(currentCharges = 7)
        val viewModel = viewModel(HelpFakeCommunityRepository("comm-1"), userRepository = userRepository)

        assertEquals(7, viewModel.uiState.value.availableCharges)
    }

    @Test
    fun `tapping a peer opens the nudge picker for them`() {
        val viewModel = viewModel(HelpFakeCommunityRepository("comm-1"))
        val peer = member("peer", MemberStatus.CRISIS)

        viewModel.onMemberClicked(peer)

        assertEquals(peer, viewModel.uiState.value.nudgeTarget)
    }

    @Test
    fun `dismissing the nudge picker clears the target`() {
        val viewModel = viewModel(HelpFakeCommunityRepository("comm-1"))
        viewModel.onMemberClicked(member("peer", MemberStatus.CRISIS))

        viewModel.onNudgePickerDismissed()

        assertNull(viewModel.uiState.value.nudgeTarget)
    }

    @Test
    fun `a peer who recovers while their picker is open stops being a target`() {
        val roster = rosterOf(member(SELF_ID), member("peer", MemberStatus.CRISIS))
        val viewModel = viewModel(HelpFakeCommunityRepository("comm-1", roster))
        viewModel.onMemberClicked(viewModel.uiState.value.peersNeedingHelp.single())

        roster.value = CommunityState(
            "comm-1",
            emptyList(),
            cohesionBonusApplied = false,
            members = listOf(member(SELF_ID), member("peer", MemberStatus.OK))
        )

        assertTrue(viewModel.uiState.value.peersNeedingHelp.isEmpty())
        assertNull(viewModel.uiState.value.nudgeTarget)
    }

    @Test
    fun `sending a nudge to a populated community delivers it and shows confirmation`() {
        val nudgeRepository = HelpFakeNudgeRepository()
        val communityRepository = HelpFakeCommunityRepository("comm-1", memberCount = 2)
        val viewModel = viewModel(communityRepository, nudgeRepository = nudgeRepository)
        viewModel.onMemberClicked(member("peer", MemberStatus.CRISIS))

        viewModel.onNudgeSelected(Nudge.Haptic)

        assertEquals("peer", nudgeRepository.lastSentTarget)
        assertEquals(Nudge.Haptic, nudgeRepository.lastSentNudge)
        assertEquals("Nudge sent to nick-peer", viewModel.uiState.value.nudgeFeedback)
        assertNull(viewModel.uiState.value.nudgeTarget)
    }

    @Test
    fun `sending a nudge while alone shows the no-peers error and sends nothing`() {
        val nudgeRepository = HelpFakeNudgeRepository()
        val communityRepository = HelpFakeCommunityRepository("comm-1", memberCount = 1)
        val viewModel = viewModel(communityRepository, nudgeRepository = nudgeRepository)
        viewModel.onMemberClicked(member("peer", MemberStatus.CRISIS))

        viewModel.onNudgeSelected(Nudge.Haptic)

        assertNull(nudgeRepository.lastSentNudge)
        assertEquals("No peers to notify", viewModel.uiState.value.nudgeFeedback)
    }

    @Test
    fun `sending a nudge with insufficient charges shows the charge error`() {
        val communityRepository = HelpFakeCommunityRepository("comm-1", memberCount = 2)
        val userRepository = HelpFakeUserRepository(currentCharges = 0)
        val viewModel = viewModel(communityRepository, userRepository = userRepository)
        viewModel.onMemberClicked(member("peer", MemberStatus.CRISIS))

        viewModel.onNudgeSelected(Nudge.Haptic)

        assertEquals(false, viewModel.uiState.value.isSendingNudge)
        assertTrue(viewModel.uiState.value.nudgeFeedback?.contains("charges") == true)
    }

    @Test
    fun `dismissing nudge feedback clears it`() {
        val communityRepository = HelpFakeCommunityRepository("comm-1", memberCount = 2)
        val viewModel = viewModel(communityRepository)
        viewModel.onMemberClicked(member("peer", MemberStatus.CRISIS))
        viewModel.onNudgeSelected(Nudge.Haptic)

        viewModel.onNudgeFeedbackShown()

        assertNull(viewModel.uiState.value.nudgeFeedback)
    }
}
