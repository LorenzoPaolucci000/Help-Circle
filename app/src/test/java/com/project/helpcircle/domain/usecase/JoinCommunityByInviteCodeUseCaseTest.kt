package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.repository.CommunityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class JoinByInviteCodeFakeRepository(
    private val matchingCode: String? = null
) : CommunityRepository {
    var lastLookedUpInviteCode: String? = null

    override fun observeCommunityState(communityId: String): Flow<CommunityState> =
        flowOf(CommunityState(communityId, emptyList(), cohesionBonusApplied = false))

    override suspend fun joinCommunity(communityId: String): CommunityState =
        CommunityState(communityId, emptyList(), cohesionBonusApplied = false)

    override suspend fun createCommunity(inviteCode: String): CommunityState =
        CommunityState("generated-id", emptyList(), cohesionBonusApplied = false, inviteCode = inviteCode)

    override suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState? {
        lastLookedUpInviteCode = inviteCode
        return if (inviteCode == matchingCode) {
            CommunityState("matched-id", emptyList(), cohesionBonusApplied = false, inviteCode = inviteCode)
        } else {
            null
        }
    }

    override suspend fun reportCrisis(communityId: String) = Unit
    override suspend fun reportRecovery(communityId: String) = Unit
    override suspend fun leaveCommunity(communityId: String) = Unit
    override suspend fun getActiveCommunityId(): String? = null
    override suspend fun getMemberCount(communityId: String): Int = 0
}

class JoinCommunityByInviteCodeUseCaseTest {

    @Test
    fun `normalizes the code to uppercase with no surrounding whitespace before looking it up`() = runBlocking {
        val repository = JoinByInviteCodeFakeRepository(matchingCode = "AB12CD")

        JoinCommunityByInviteCodeUseCase(repository)("  ab12cd  ")

        assertEquals("AB12CD", repository.lastLookedUpInviteCode)
    }

    @Test
    fun `returns the joined community state on a matching code`() = runBlocking {
        val repository = JoinByInviteCodeFakeRepository(matchingCode = "AB12CD")

        val state = JoinCommunityByInviteCodeUseCase(repository)("AB12CD")

        assertEquals("matched-id", state?.communityId)
    }

    @Test
    fun `returns null when no community has that code`() = runBlocking {
        val repository = JoinByInviteCodeFakeRepository(matchingCode = "AB12CD")

        val state = JoinCommunityByInviteCodeUseCase(repository)("ZZ99ZZ")

        assertNull(state)
    }
}
