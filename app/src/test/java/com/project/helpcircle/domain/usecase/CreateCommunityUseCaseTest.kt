package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.repository.CommunityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class CreateCommunityFakeRepository : CommunityRepository {
    var lastCreatedInviteCode: String? = null

    override fun observeCommunityState(communityId: String): Flow<CommunityState> =
        flowOf(CommunityState(communityId, emptyList(), cohesionBonusApplied = false))

    override suspend fun joinCommunity(communityId: String): CommunityState =
        CommunityState(communityId, emptyList(), cohesionBonusApplied = false)

    override suspend fun createCommunity(inviteCode: String): CommunityState {
        lastCreatedInviteCode = inviteCode
        return CommunityState("generated-id", emptyList(), cohesionBonusApplied = false, inviteCode = inviteCode)
    }

    override suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState? = null
    override suspend fun reportCrisis(communityId: String) = Unit
    override suspend fun reportRecovery(communityId: String) = Unit
    override suspend fun leaveCommunity(communityId: String) = Unit
    override suspend fun getActiveCommunityId(): String? = null
    override suspend fun getMemberCount(communityId: String): Int = 0
}

class CreateCommunityUseCaseTest {

    @Test
    fun `creates a community with a freshly generated 6-character invite code`() = runBlocking {
        val repository = CreateCommunityFakeRepository()

        val state = CreateCommunityUseCase(repository)()

        assertEquals(repository.lastCreatedInviteCode, state.inviteCode)
        assertTrue(state.inviteCode.matches(Regex("^[A-Z0-9]{6}$")))
    }
}
