package com.project.helpcircle.presentation.community

import com.project.helpcircle.domain.engine.ForegroundAppTracker
import com.project.helpcircle.domain.engine.WeeklyResetCalculator
import com.project.helpcircle.domain.model.ChargeWallet
import com.project.helpcircle.domain.model.CommunityMember
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.CommunityWeeklySummary
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.model.UserIdentity
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.CommunityWeeklyHistoryRepository
import com.project.helpcircle.domain.repository.NudgeRepository
import com.project.helpcircle.domain.repository.UserRepository
import com.project.helpcircle.domain.usecase.ConsumeChargeUseCase
import com.project.helpcircle.domain.usecase.IsFocusModeActiveUseCase
import com.project.helpcircle.domain.usecase.ObserveChargeWalletUseCase
import com.project.helpcircle.domain.usecase.ObserveCommunityStateUseCase
import com.project.helpcircle.domain.usecase.ObserveCommunityWeeklyTrendUseCase
import com.project.helpcircle.domain.usecase.ObserveIncomingNudgesUseCase
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

private const val FROZEN_LAST_REPLENISHED_AT = Long.MAX_VALUE / 2

private class DashboardFakeCommunityRepository(
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

private class DashboardFakeUserRepository(currentCharges: Int) : UserRepository {
    val walletFlow = MutableStateFlow(
        ChargeWallet(currentCharges = currentCharges, lastReplenishedAtEpochMillis = FROZEN_LAST_REPLENISHED_AT)
    )
    override suspend fun getOrCreateIdentity(): UserIdentity = UserIdentity(anonymousHash = "uid", nickname = "nick")
    override suspend fun saveNickname(nickname: String) = Unit
    override val chargeWallet: Flow<ChargeWallet> = walletFlow
    override suspend fun updateChargeWallet(wallet: ChargeWallet) {
        walletFlow.value = wallet
    }
}

private class DashboardFakeCommunityWeeklyHistoryRepository(
    initialSummaries: List<CommunityWeeklySummary> = emptyList()
) : CommunityWeeklyHistoryRepository {
    private val summariesFlow = MutableStateFlow(initialSummaries)
    override fun weeklySummaries(communityId: String): Flow<List<CommunityWeeklySummary>> = summariesFlow
    override suspend fun ensureWeeklySnapshotApplied(communityId: String, currentCollectiveIndexValue: Int) = Unit
}

private class DashboardFakeNudgeRepository : NudgeRepository {
    val incomingFlow = MutableSharedFlow<Nudge>(extraBufferCapacity = 1)
    var lastSentTarget: String? = null
    var lastSentNudge: Nudge? = null

    override val incomingNudges: Flow<Nudge> = incomingFlow
    override suspend fun sendNudge(communityId: String, targetUserId: String, nudge: Nudge) {
        lastSentTarget = targetUserId
        lastSentNudge = nudge
    }
}

private fun member(
    id: String,
    status: MemberStatus = MemberStatus.OK,
    score: Int = 50,
    satisfaction: WeeklySatisfaction? = null,
    satisfactionWeekStart: Long? = null
) = CommunityMember(id, "nick-$id", status, score, satisfaction, satisfactionWeekStart)

/** The week a rating must be stamped with to count right now — the same value the ViewModel derives internally. */
private fun currentWeekStart(): Long =
    WeeklyResetCalculator.currentWeekStartEpochMillis(System.currentTimeMillis())

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityDashboardViewModelTest {

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
        userRepository: UserRepository = DashboardFakeUserRepository(currentCharges = 5),
        nudgeRepository: NudgeRepository = DashboardFakeNudgeRepository(),
        communityWeeklyHistoryRepository: CommunityWeeklyHistoryRepository = DashboardFakeCommunityWeeklyHistoryRepository()
    ): CommunityDashboardViewModel {
        val consumeCharge = ConsumeChargeUseCase(
            userRepository,
            ObserveChargeWalletUseCase(userRepository, IsFocusModeActiveUseCase(ForegroundAppTracker()))
        )
        return CommunityDashboardViewModel(
            communityRepository,
            ObserveChargeWalletUseCase(userRepository, IsFocusModeActiveUseCase(ForegroundAppTracker())),
            ObserveCommunityStateUseCase(communityRepository),
            ObserveCommunityWeeklyTrendUseCase(communityWeeklyHistoryRepository),
            ObserveIncomingNudgesUseCase(nudgeRepository),
            SendNudgeUseCase(nudgeRepository, communityRepository, consumeCharge)
        )
    }

    @Test
    fun `no active community shows the no-circle state`() {
        val viewModel = viewModel(DashboardFakeCommunityRepository(activeCommunityId = null))

        val state = viewModel.uiState.value
        assertEquals(false, state.hasActiveCommunity)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `a community with one member loads as solo mode`() {
        val stateFlow = MutableStateFlow(
            CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, inviteCode = "AB12CD")
        )
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        val state = viewModel.uiState.value
        assertTrue(state.isSolo)
        assertEquals("AB12CD", state.inviteCode)
        assertTrue(state.members.isEmpty())
    }

    @Test
    fun `a community with peers loads its member roster`() {
        val members = listOf(member("self"), member("peer"))
        val stateFlow = MutableStateFlow(CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, members = members))
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        val state = viewModel.uiState.value
        assertEquals(false, state.isSolo)
        assertEquals(members, state.members)
    }

    @Test
    fun `with no recorded weekly history the weekly trend fields are null`() {
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1"))

        val state = viewModel.uiState.value
        assertNull(state.latestWeeklyCollectiveIndex)
        assertNull(state.previousWeeklyCollectiveIndex)
    }

    @Test
    fun `the latest and previous weekly snapshots are reflected in the ui state`() {
        val oldest = CommunityWeeklySummary(communityId = "comm-1", weekStartEpochMillis = 1_000, collectiveIndexValue = 55)
        val newest = CommunityWeeklySummary(communityId = "comm-1", weekStartEpochMillis = 2_000, collectiveIndexValue = 68)
        val weeklyHistoryRepository = DashboardFakeCommunityWeeklyHistoryRepository(listOf(newest, oldest))
        val viewModel = viewModel(
            DashboardFakeCommunityRepository("comm-1"),
            communityWeeklyHistoryRepository = weeklyHistoryRepository
        )

        val state = viewModel.uiState.value
        assertEquals(68, state.latestWeeklyCollectiveIndex)
        assertEquals(55, state.previousWeeklyCollectiveIndex)
    }

    @Test
    fun `the circle's current-week ratings are aggregated into the ui state`() {
        val week = currentWeekStart()
        val members = listOf(
            member("self", satisfaction = WeeklySatisfaction.HAPPY, satisfactionWeekStart = week),
            member("peer", satisfaction = WeeklySatisfaction.NEUTRAL, satisfactionWeekStart = week)
        )
        val stateFlow = MutableStateFlow(CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, members = members))
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        val satisfaction = viewModel.uiState.value.communitySatisfaction
        assertEquals(2, satisfaction?.ratedMemberCount)
        assertEquals(2, satisfaction?.memberCount)
        assertEquals(75, satisfaction?.averageScore)
    }

    @Test
    fun `a circle where nobody has rated this week reports no ratings but still counts its members`() {
        val members = listOf(member("self"), member("peer"))
        val stateFlow = MutableStateFlow(CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, members = members))
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        val satisfaction = viewModel.uiState.value.communitySatisfaction
        assertEquals(false, satisfaction?.hasRatings)
        assertEquals(2, satisfaction?.memberCount)
    }

    @Test
    fun `ratings left over from a previous week are not counted toward this week`() {
        val staleWeek = currentWeekStart() - WeeklyResetCalculator.WEEK_DURATION_MILLIS
        val members = listOf(
            member("self", satisfaction = WeeklySatisfaction.HAPPY, satisfactionWeekStart = staleWeek),
            member("peer", satisfaction = WeeklySatisfaction.BAD, satisfactionWeekStart = currentWeekStart())
        )
        val stateFlow = MutableStateFlow(CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, members = members))
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        val satisfaction = viewModel.uiState.value.communitySatisfaction
        assertEquals(1, satisfaction?.ratedMemberCount)
        assertEquals(0, satisfaction?.averageScore)
    }

    @Test
    fun `solo mode reports no community satisfaction at all`() {
        val stateFlow = MutableStateFlow(
            CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, inviteCode = "AB12CD")
        )
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        assertTrue(viewModel.uiState.value.isSolo)
        assertNull(viewModel.uiState.value.communitySatisfaction)
    }

    @Test
    fun `the charge wallet's current balance is reflected in the ui state`() {
        val userRepository = DashboardFakeUserRepository(currentCharges = 7)
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1"), userRepository = userRepository)

        assertEquals(7, viewModel.uiState.value.availableCharges)
    }

    @Test
    fun `an incoming nudge updates the latest-nudge state`() {
        val nudgeRepository = DashboardFakeNudgeRepository()
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1"), nudgeRepository = nudgeRepository)

        nudgeRepository.incomingFlow.tryEmit(Nudge.Haptic)

        assertEquals(Nudge.Haptic, viewModel.uiState.value.latestNudge)
    }

    @Test
    fun `tapping a member opens the nudge picker for them`() {
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1"))
        val peer = member("peer")

        viewModel.onMemberClicked(peer)

        assertEquals(peer, viewModel.uiState.value.nudgeTarget)
    }

    @Test
    fun `dismissing the nudge picker clears the target`() {
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1"))
        viewModel.onMemberClicked(member("peer"))

        viewModel.onNudgePickerDismissed()

        assertNull(viewModel.uiState.value.nudgeTarget)
    }

    @Test
    fun `sending a nudge to a populated community delivers it and shows confirmation`() {
        val nudgeRepository = DashboardFakeNudgeRepository()
        val communityRepository = DashboardFakeCommunityRepository("comm-1", memberCount = 2)
        val viewModel = viewModel(communityRepository, nudgeRepository = nudgeRepository)
        viewModel.onMemberClicked(member("peer"))

        viewModel.onNudgeSelected(Nudge.Haptic)

        assertEquals("peer", nudgeRepository.lastSentTarget)
        assertEquals(Nudge.Haptic, nudgeRepository.lastSentNudge)
        assertEquals("Nudge sent to nick-peer", viewModel.uiState.value.nudgeFeedback)
        assertNull(viewModel.uiState.value.nudgeTarget)
    }

    @Test
    fun `sending a nudge while alone shows the no-peers error and sends nothing`() {
        val nudgeRepository = DashboardFakeNudgeRepository()
        val communityRepository = DashboardFakeCommunityRepository("comm-1", memberCount = 1)
        val viewModel = viewModel(communityRepository, nudgeRepository = nudgeRepository)
        viewModel.onMemberClicked(member("peer"))

        viewModel.onNudgeSelected(Nudge.Haptic)

        assertNull(nudgeRepository.lastSentNudge)
        assertEquals("No peers to notify", viewModel.uiState.value.nudgeFeedback)
    }

    @Test
    fun `sending a nudge with insufficient charges shows the charge error`() {
        val communityRepository = DashboardFakeCommunityRepository("comm-1", memberCount = 2)
        val userRepository = DashboardFakeUserRepository(currentCharges = 0)
        val viewModel = viewModel(communityRepository, userRepository = userRepository)
        viewModel.onMemberClicked(member("peer"))

        viewModel.onNudgeSelected(Nudge.Haptic)

        assertEquals(false, viewModel.uiState.value.isSendingNudge)
        assertTrue(viewModel.uiState.value.nudgeFeedback?.contains("charges") == true)
    }

    @Test
    fun `dismissing nudge feedback clears it`() {
        val communityRepository = DashboardFakeCommunityRepository("comm-1", memberCount = 2)
        val viewModel = viewModel(communityRepository)
        viewModel.onMemberClicked(member("peer"))
        viewModel.onNudgeSelected(Nudge.Haptic)

        viewModel.onNudgeFeedbackShown()

        assertNull(viewModel.uiState.value.nudgeFeedback)
    }
}
