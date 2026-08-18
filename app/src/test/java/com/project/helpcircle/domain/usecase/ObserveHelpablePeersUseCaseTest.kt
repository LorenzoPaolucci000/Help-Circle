package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.ChargeWallet
import com.project.helpcircle.domain.model.CommunityMember
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.model.UserIdentity
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class HelpablePeersFakeCommunityRepository(
    initialState: CommunityState
) : CommunityRepository {
    private val stateFlow = MutableStateFlow(initialState)

    override fun observeCommunityState(communityId: String): Flow<CommunityState> = stateFlow

    override suspend fun joinCommunity(communityId: String): CommunityState = stateFlow.value
    override suspend fun createCommunity(communityId: String, inviteCode: String, name: String): CommunityState = stateFlow.value
    override suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState? = null
    override suspend fun reportCrisis(communityId: String) = Unit
    override suspend fun publishStatus(communityId: String, status: MemberStatus) = Unit
    override suspend fun reportRecovery(communityId: String) = Unit
    override suspend fun publishSatisfaction(
        communityId: String,
        weekStartEpochMillis: Long,
        satisfaction: WeeklySatisfaction
    ) = Unit
    override suspend fun leaveCommunity(communityId: String) = Unit
    override suspend fun ensureAlertSubscription() = Unit
    override suspend fun getActiveCommunityId(): String? = "comm-1"
    override suspend fun getMemberCount(communityId: String): Int = stateFlow.value.members.size
}

private class HelpablePeersFakeUserRepository(private val anonymousHash: String) : UserRepository {
    override suspend fun getOrCreateIdentity(): UserIdentity = UserIdentity(anonymousHash, "nick-self")
    override suspend fun saveNickname(nickname: String) = Unit
    override val chargeWallet: Flow<ChargeWallet> = flowOf(ChargeWallet(ChargeWallet.MAX_CHARGES, 0L))
    override suspend fun updateChargeWallet(wallet: ChargeWallet) = Unit
}

private fun member(id: String, status: MemberStatus) =
    CommunityMember(anonymousId = id, nickname = "nick-$id", status = status, agencyScore = 50)

private fun communityState(members: List<CommunityMember>) = CommunityState(
    communityId = "comm-1",
    memberAgencyIndices = emptyList(),
    cohesionBonusApplied = false,
    members = members,
    inviteCode = "AB12CD"
)

class ObserveHelpablePeersUseCaseTest {

    private fun useCase(members: List<CommunityMember>, selfId: String = "self") = ObserveHelpablePeersUseCase(
        observeCommunityStateUseCase = ObserveCommunityStateUseCase(
            HelpablePeersFakeCommunityRepository(communityState(members))
        ),
        userRepository = HelpablePeersFakeUserRepository(selfId)
    )

    @Test
    fun `emits the at-risk and crisis peers of a populated community`() = runBlocking {
        val useCase = useCase(
            listOf(
                member("self", MemberStatus.CRISIS),
                member("calm", MemberStatus.OK),
                member("slipping", MemberStatus.AT_RISK),
                member("struggling", MemberStatus.CRISIS)
            )
        )

        val helpable = useCase("comm-1").first()

        assertEquals(listOf("struggling", "slipping"), helpable.peers.map { it.anonymousId })
        assertEquals(3, helpable.totalPeerCount)
    }

    @Test
    fun `emits nothing helpable while this device is alone in its circle`() = runBlocking {
        val useCase = useCase(listOf(member("self", MemberStatus.CRISIS)))

        val helpable = useCase("comm-1").first()

        assertTrue(helpable.peers.isEmpty())
        assertEquals(0, helpable.totalPeerCount)
    }

    @Test
    fun `emits no helpable peers when every peer is doing fine`() = runBlocking {
        val useCase = useCase(
            listOf(
                member("self", MemberStatus.OK),
                member("calm-one", MemberStatus.OK),
                member("calm-two", MemberStatus.OK)
            )
        )

        val helpable = useCase("comm-1").first()

        assertTrue(helpable.peers.isEmpty())
        assertEquals(2, helpable.totalPeerCount)
    }
}
