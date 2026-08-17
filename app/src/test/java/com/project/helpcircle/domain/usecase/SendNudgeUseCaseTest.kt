package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.ForegroundAppTracker
import com.project.helpcircle.domain.model.ChargeWallet
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.model.UserIdentity
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.NudgeRepository
import com.project.helpcircle.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val FROZEN_LAST_REPLENISHED_AT = Long.MAX_VALUE / 2

private class SendNudgeFakeUserRepository(currentCharges: Int) : UserRepository {
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

private class SendNudgeFakeCommunityRepository(private val memberCount: Int) : CommunityRepository {
    override fun observeCommunityState(communityId: String): Flow<CommunityState> = emptyFlow()
    override suspend fun joinCommunity(communityId: String): CommunityState =
        throw UnsupportedOperationException()
    override suspend fun createCommunity(communityId: String, inviteCode: String, name: String): CommunityState =
        throw UnsupportedOperationException()
    override suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState? = null
    override suspend fun reportCrisis(communityId: String) = Unit
    override suspend fun reportRecovery(communityId: String) = Unit
    override suspend fun publishSatisfaction(
        communityId: String,
        weekStartEpochMillis: Long,
        satisfaction: WeeklySatisfaction
    ) = Unit
    override suspend fun leaveCommunity(communityId: String) = Unit
    override suspend fun getActiveCommunityId(): String? = null
    override suspend fun getMemberCount(communityId: String): Int = memberCount
}

private class SendNudgeFakeNudgeRepository : NudgeRepository {
    var lastCommunityId: String? = null
    var lastTargetUserId: String? = null
    var lastNudge: Nudge? = null

    override val incomingNudges: Flow<Nudge> = emptyFlow()

    override suspend fun sendNudge(communityId: String, targetUserId: String, nudge: Nudge) {
        lastCommunityId = communityId
        lastTargetUserId = targetUserId
        lastNudge = nudge
    }
}

private fun sendNudgeUseCase(
    nudgeRepository: NudgeRepository,
    communityRepository: CommunityRepository,
    userRepository: UserRepository
) = SendNudgeUseCase(
    nudgeRepository,
    communityRepository,
    ConsumeChargeUseCase(userRepository, ObserveChargeWalletUseCase(userRepository, IsFocusModeActiveUseCase(ForegroundAppTracker())))
)

class SendNudgeUseCaseTest {

    @Test
    fun `refuses to send and spends nothing when the caller is alone in the community`() = runBlocking {
        val nudgeRepository = SendNudgeFakeNudgeRepository()
        val userRepository = SendNudgeFakeUserRepository(currentCharges = 5)
        val useCase = sendNudgeUseCase(nudgeRepository, SendNudgeFakeCommunityRepository(memberCount = 1), userRepository)

        val result = useCase("comm-1", "peer", Nudge.Haptic)

        assertEquals(NudgeResult.Error("No peers to notify"), result)
        assertEquals(null, nudgeRepository.lastNudge)
        assertEquals(5, userRepository.walletFlow.value.currentCharges)
    }

    @Test
    fun `sends the nudge and spends its charge cost when peers are present`() = runBlocking {
        val nudgeRepository = SendNudgeFakeNudgeRepository()
        val userRepository = SendNudgeFakeUserRepository(currentCharges = 5)
        val useCase = sendNudgeUseCase(nudgeRepository, SendNudgeFakeCommunityRepository(memberCount = 2), userRepository)

        val result = useCase("comm-1", "peer", Nudge.Haptic)

        assertTrue(result is NudgeResult.Sent)
        assertEquals("comm-1", nudgeRepository.lastCommunityId)
        assertEquals("peer", nudgeRepository.lastTargetUserId)
        assertEquals(Nudge.Haptic, nudgeRepository.lastNudge)
        assertEquals(3, userRepository.walletFlow.value.currentCharges)
    }

    @Test
    fun `does not send when there are peers but not enough charges`() = runBlocking {
        val nudgeRepository = SendNudgeFakeNudgeRepository()
        val userRepository = SendNudgeFakeUserRepository(currentCharges = 0)
        val useCase = sendNudgeUseCase(nudgeRepository, SendNudgeFakeCommunityRepository(memberCount = 2), userRepository)

        var threw = false
        try {
            useCase("comm-1", "peer", Nudge.Haptic)
        } catch (e: IllegalStateException) {
            threw = true
        }

        assertTrue(threw)
        assertEquals(null, nudgeRepository.lastNudge)
    }
}
