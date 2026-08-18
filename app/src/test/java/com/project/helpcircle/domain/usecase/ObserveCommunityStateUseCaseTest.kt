package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityMember
import com.project.helpcircle.domain.model.CommunityObservation
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.CommunityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class ObserveCommunityStateFakeRepository(
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
    override suspend fun getActiveCommunityId(): String? = null
    override suspend fun getMemberCount(communityId: String): Int = stateFlow.value.members.size
}

private fun member(id: String) = CommunityMember(id, "nickname-$id", MemberStatus.OK, agencyScore = 50)

class ObserveCommunityStateUseCaseTest {

    @Test
    fun `treats a community with no members as solo mode`() = runBlocking {
        val state = CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, inviteCode = "AB12CD")
        val useCase = ObserveCommunityStateUseCase(ObserveCommunityStateFakeRepository(state))

        val observation = useCase("comm-1").first()

        assertTrue(observation is CommunityObservation.SoloMode)
        observation as CommunityObservation.SoloMode
        assertEquals("comm-1", observation.communityId)
        assertEquals("AB12CD", observation.inviteCode)
    }

    @Test
    fun `treats a community with exactly one member as solo mode`() = runBlocking {
        val state = CommunityState(
            "comm-1",
            emptyList(),
            cohesionBonusApplied = false,
            members = listOf(member("self")),
            inviteCode = "AB12CD"
        )
        val useCase = ObserveCommunityStateUseCase(ObserveCommunityStateFakeRepository(state))

        val observation = useCase("comm-1").first()

        assertTrue(observation is CommunityObservation.SoloMode)
    }

    @Test
    fun `treats a community with two or more members as populated`() = runBlocking {
        val state = CommunityState(
            "comm-1",
            emptyList(),
            cohesionBonusApplied = false,
            members = listOf(member("self"), member("peer")),
            inviteCode = "AB12CD"
        )
        val useCase = ObserveCommunityStateUseCase(ObserveCommunityStateFakeRepository(state))

        val observation = useCase("comm-1").first()

        assertTrue(observation is CommunityObservation.Populated)
        observation as CommunityObservation.Populated
        assertEquals(state, observation.state)
    }
}
