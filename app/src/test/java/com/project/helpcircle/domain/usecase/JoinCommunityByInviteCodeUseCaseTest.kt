package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
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

    override suspend fun createCommunity(communityId: String, inviteCode: String): CommunityState =
        CommunityState(communityId, emptyList(), cohesionBonusApplied = false, inviteCode = inviteCode)

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
    override suspend fun publishSatisfaction(
        communityId: String,
        weekStartEpochMillis: Long,
        satisfaction: WeeklySatisfaction
    ) = Unit
    override suspend fun leaveCommunity(communityId: String) = Unit
    override suspend fun getActiveCommunityId(): String? = null
    override suspend fun getMemberCount(communityId: String): Int = 0
}

private class JoinByInviteCodeFakeMonitoredAppsRepository(
    monitoredPackageNames: Set<String> = setOf("com.example.social")
) : MonitoredAppsRepository {
    override val monitoredPackageNames: Flow<Set<String>> = flowOf(monitoredPackageNames)
    override suspend fun setMonitored(packageName: String, isMonitored: Boolean) = Unit
    override suspend fun isMonitored(packageName: String): Boolean = false
}

class JoinCommunityByInviteCodeUseCaseTest {

    @Test
    fun `normalizes the code to uppercase with no surrounding whitespace before looking it up`() = runBlocking {
        val repository = JoinByInviteCodeFakeRepository(matchingCode = "AB12CD")

        JoinCommunityByInviteCodeUseCase(repository, JoinByInviteCodeFakeMonitoredAppsRepository())("  ab12cd  ")

        assertEquals("AB12CD", repository.lastLookedUpInviteCode)
    }

    @Test
    fun `returns the joined community state on a matching code`() = runBlocking {
        val repository = JoinByInviteCodeFakeRepository(matchingCode = "AB12CD")

        val state = JoinCommunityByInviteCodeUseCase(repository, JoinByInviteCodeFakeMonitoredAppsRepository())("AB12CD")

        assertEquals("matched-id", state?.communityId)
    }

    @Test
    fun `returns null when no community has that code`() = runBlocking {
        val repository = JoinByInviteCodeFakeRepository(matchingCode = "AB12CD")

        val state = JoinCommunityByInviteCodeUseCase(repository, JoinByInviteCodeFakeMonitoredAppsRepository())("ZZ99ZZ")

        assertNull(state)
    }

    @Test
    fun `fails with no monitored apps before ever looking up the code`() = runBlocking {
        val repository = JoinByInviteCodeFakeRepository(matchingCode = "AB12CD")
        val monitoredAppsRepository = JoinByInviteCodeFakeMonitoredAppsRepository(monitoredPackageNames = emptySet())

        val exception = try {
            JoinCommunityByInviteCodeUseCase(repository, monitoredAppsRepository)("AB12CD")
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertEquals("Add at least one app to monitor first", exception?.message)
        assertNull(repository.lastLookedUpInviteCode)
    }
}
