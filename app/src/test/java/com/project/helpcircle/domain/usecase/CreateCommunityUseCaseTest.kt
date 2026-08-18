package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class CreateCommunityFakeRepository(var throwOnCreate: Boolean = false) : CommunityRepository {
    var lastCreatedInviteCode: String? = null
    val createdCommunityIds = mutableListOf<String>()
    val createdNames = mutableListOf<String>()

    override fun observeCommunityState(communityId: String): Flow<CommunityState> =
        flowOf(CommunityState(communityId, emptyList(), cohesionBonusApplied = false))

    override suspend fun joinCommunity(communityId: String): CommunityState =
        CommunityState(communityId, emptyList(), cohesionBonusApplied = false)

    override suspend fun createCommunity(communityId: String, inviteCode: String, name: String): CommunityState {
        createdCommunityIds += communityId
        createdNames += name
        if (throwOnCreate) throw RuntimeException("boom")
        lastCreatedInviteCode = inviteCode
        return CommunityState(
            communityId,
            emptyList(),
            cohesionBonusApplied = false,
            inviteCode = inviteCode,
            name = name
        )
    }

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
    override suspend fun getActiveCommunityId(): String? = null
    override suspend fun getMemberCount(communityId: String): Int = 0
}

private class CreateCommunityFakeMonitoredAppsRepository(
    monitoredPackageNames: Set<String> = setOf("com.example.social")
) : MonitoredAppsRepository {
    override val monitoredPackageNames: Flow<Set<String>> = flowOf(monitoredPackageNames)
    override suspend fun setMonitored(packageName: String, isMonitored: Boolean) = Unit
    override suspend fun isMonitored(packageName: String): Boolean = false
}

class CreateCommunityUseCaseTest {

    @Test
    fun `creates a community with a freshly generated 6-character invite code`() = runBlocking {
        val repository = CreateCommunityFakeRepository()

        val state = CreateCommunityUseCase(repository, CreateCommunityFakeMonitoredAppsRepository())()

        assertEquals(repository.lastCreatedInviteCode, state.inviteCode)
        assertTrue(state.inviteCode.matches(Regex("^[A-Z0-9]{6}$")))
    }

    @Test
    fun `fails with no monitored apps before ever creating a community`() = runBlocking {
        val repository = CreateCommunityFakeRepository()
        val monitoredAppsRepository = CreateCommunityFakeMonitoredAppsRepository(monitoredPackageNames = emptySet())

        val exception = try {
            CreateCommunityUseCase(repository, monitoredAppsRepository)()
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertEquals("Add at least one app to monitor first", exception?.message)
        assertNull(repository.lastCreatedInviteCode)
    }

    @Test
    fun `retrying after a failed attempt reuses the same community ID and invite code`() = runBlocking {
        val repository = CreateCommunityFakeRepository(throwOnCreate = true)
        val useCase = CreateCommunityUseCase(repository, CreateCommunityFakeMonitoredAppsRepository())

        val firstAttemptFailed = try {
            useCase()
            false
        } catch (e: RuntimeException) {
            true
        }
        repository.throwOnCreate = false
        val state = useCase()

        assertTrue(firstAttemptFailed)
        assertEquals(2, repository.createdCommunityIds.size)
        assertEquals(repository.createdCommunityIds[0], repository.createdCommunityIds[1])
        assertEquals(repository.createdCommunityIds[1], state.communityId)
    }

    @Test
    fun `passes the chosen name through, trimmed`() = runBlocking {
        val repository = CreateCommunityFakeRepository()

        val state = CreateCommunityUseCase(repository, CreateCommunityFakeMonitoredAppsRepository())("  OpenHarbor42  ")

        assertEquals("OpenHarbor42", repository.createdNames.single())
        assertEquals("OpenHarbor42", state.name)
    }

    @Test
    fun `generates a valid name when none is supplied`() = runBlocking {
        val repository = CreateCommunityFakeRepository()

        val state = CreateCommunityUseCase(repository, CreateCommunityFakeMonitoredAppsRepository())()

        assertEquals(NicknameValidationResult.Valid, ValidateNicknameUseCase()(state.name))
        assertEquals(repository.createdNames.single(), state.name)
    }

    @Test
    fun `retrying after a failed attempt reuses the same generated name`() = runBlocking {
        val repository = CreateCommunityFakeRepository(throwOnCreate = true)
        val useCase = CreateCommunityUseCase(repository, CreateCommunityFakeMonitoredAppsRepository())

        try {
            useCase()
        } catch (e: RuntimeException) {
            // Expected: the point is what the retry below writes, not this failure.
        }
        repository.throwOnCreate = false
        useCase()

        assertEquals(2, repository.createdNames.size)
        assertEquals(repository.createdNames[0], repository.createdNames[1])
    }

    @Test
    fun `a successful create does not reuse its ID on a later, unrelated attempt`() = runBlocking {
        val repository = CreateCommunityFakeRepository()
        val useCase = CreateCommunityUseCase(repository, CreateCommunityFakeMonitoredAppsRepository())

        useCase()
        useCase()

        assertEquals(2, repository.createdCommunityIds.size)
        assertTrue(repository.createdCommunityIds[0] != repository.createdCommunityIds[1])
    }
}
