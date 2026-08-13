package com.project.helpcircle.presentation.community

import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import com.project.helpcircle.domain.usecase.CreateCommunityUseCase
import com.project.helpcircle.domain.usecase.JoinCommunityByInviteCodeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class JoinCommunityFakeRepository(
    private val joinResult: CommunityState? = null,
    private val joinThrows: Boolean = false,
    private val createResult: CommunityState = CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, inviteCode = "AB12CD"),
    private val createThrows: Boolean = false
) : CommunityRepository {
    override fun observeCommunityState(communityId: String): Flow<CommunityState> = emptyFlow()
    override suspend fun joinCommunity(communityId: String): CommunityState = throw UnsupportedOperationException()

    override suspend fun createCommunity(communityId: String, inviteCode: String): CommunityState {
        // A plain RuntimeException here, deliberately not IllegalStateException, so this stays
        // distinct from the use case's own blacklist-check failure and correctly exercises the
        // ViewModel's generic-exception fallback path rather than colliding with it.
        if (createThrows) throw RuntimeException("boom")
        return createResult
    }

    override suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState? {
        if (joinThrows) throw RuntimeException("boom")
        return joinResult
    }

    override suspend fun reportCrisis(communityId: String) = Unit
    override suspend fun reportRecovery(communityId: String) = Unit
    override suspend fun leaveCommunity(communityId: String) = Unit
    override suspend fun getActiveCommunityId(): String? = null
    override suspend fun getMemberCount(communityId: String): Int = 0
}

private class JoinCommunityFakeMonitoredAppsRepository(
    monitoredPackageNames: Set<String> = setOf("com.example.social")
) : MonitoredAppsRepository {
    override val monitoredPackageNames: Flow<Set<String>> = flowOf(monitoredPackageNames)
    override suspend fun setMonitored(packageName: String, isMonitored: Boolean) = Unit
    override suspend fun isMonitored(packageName: String): Boolean = false
}

private fun viewModel(
    repository: CommunityRepository,
    monitoredAppsRepository: MonitoredAppsRepository = JoinCommunityFakeMonitoredAppsRepository()
) = JoinCommunityViewModel(
    JoinCommunityByInviteCodeUseCase(repository, monitoredAppsRepository),
    CreateCommunityUseCase(repository, monitoredAppsRepository),
    monitoredAppsRepository
)

@OptIn(ExperimentalCoroutinesApi::class)
class JoinCommunityViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selecting a tab updates which one is active`() {
        val viewModel = viewModel(JoinCommunityFakeRepository())

        viewModel.onTabSelected(JoinCommunityTab.CREATE)

        assertEquals(JoinCommunityTab.CREATE, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `editing the invite code clears any prior join error`() {
        val viewModel = viewModel(JoinCommunityFakeRepository())
        viewModel.onJoinClicked()

        viewModel.onInviteCodeInputChanged("AB12CD")

        assertEquals("AB12CD", viewModel.uiState.value.inviteCodeInput)
        assertNull(viewModel.uiState.value.joinError)
    }

    @Test
    fun `joining with a blank code does nothing`() {
        val viewModel = viewModel(JoinCommunityFakeRepository())
        viewModel.onInviteCodeInputChanged("   ")

        viewModel.onJoinClicked()

        assertEquals(false, viewModel.uiState.value.isJoining)
        assertEquals(false, viewModel.uiState.value.hasJoined)
    }

    @Test
    fun `joining a matching code marks the circle as joined`() {
        val matched = CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, inviteCode = "AB12CD")
        val viewModel = viewModel(JoinCommunityFakeRepository(joinResult = matched))
        viewModel.onInviteCodeInputChanged("AB12CD")

        viewModel.onJoinClicked()

        assertTrue(viewModel.uiState.value.hasJoined)
        assertEquals(false, viewModel.uiState.value.isJoining)
    }

    @Test
    fun `joining a code with no match shows a not-found error`() {
        val viewModel = viewModel(JoinCommunityFakeRepository(joinResult = null))
        viewModel.onInviteCodeInputChanged("ZZ99ZZ")

        viewModel.onJoinClicked()

        assertEquals("Code not found", viewModel.uiState.value.joinError)
        assertEquals(false, viewModel.uiState.value.hasJoined)
    }

    @Test
    fun `a repository failure while joining shows a generic retry error`() {
        val viewModel = viewModel(JoinCommunityFakeRepository(joinThrows = true))
        viewModel.onInviteCodeInputChanged("AB12CD")

        viewModel.onJoinClicked()

        assertEquals("Couldn't join that circle. Try again.", viewModel.uiState.value.joinError)
    }

    @Test
    fun `creating a circle stores its generated invite code`() {
        val created = CommunityState("comm-2", emptyList(), cohesionBonusApplied = false, inviteCode = "ZZ99ZZ")
        val viewModel = viewModel(JoinCommunityFakeRepository(createResult = created))

        viewModel.onCreateClicked()

        assertEquals("ZZ99ZZ", viewModel.uiState.value.createdInviteCode)
        assertEquals(false, viewModel.uiState.value.isCreating)
    }

    @Test
    fun `a repository failure while creating shows a generic retry error`() {
        val viewModel = viewModel(JoinCommunityFakeRepository(createThrows = true))

        viewModel.onCreateClicked()

        assertEquals("Couldn't create a circle. Try again.", viewModel.uiState.value.createError)
    }

    @Test
    fun `continuing after create marks the circle as joined`() {
        val viewModel = viewModel(JoinCommunityFakeRepository())

        viewModel.onContinueAfterCreateClicked()

        assertTrue(viewModel.uiState.value.hasJoined)
    }

    @Test
    fun `hasMonitoredApps reflects an empty blacklist`() {
        val viewModel = viewModel(
            JoinCommunityFakeRepository(),
            monitoredAppsRepository = JoinCommunityFakeMonitoredAppsRepository(emptySet())
        )

        assertEquals(false, viewModel.uiState.value.hasMonitoredApps)
    }

    @Test
    fun `hasMonitoredApps reflects a non-empty blacklist`() {
        val viewModel = viewModel(
            JoinCommunityFakeRepository(),
            monitoredAppsRepository = JoinCommunityFakeMonitoredAppsRepository(setOf("com.example.social"))
        )

        assertEquals(true, viewModel.uiState.value.hasMonitoredApps)
    }

    @Test
    fun `joining with no monitored apps surfaces the use case's blacklist error`() {
        val matched = CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, inviteCode = "AB12CD")
        val viewModel = viewModel(
            JoinCommunityFakeRepository(joinResult = matched),
            monitoredAppsRepository = JoinCommunityFakeMonitoredAppsRepository(emptySet())
        )
        viewModel.onInviteCodeInputChanged("AB12CD")

        viewModel.onJoinClicked()

        assertEquals("Add at least one app to monitor first", viewModel.uiState.value.joinError)
        assertEquals(false, viewModel.uiState.value.hasJoined)
    }

    @Test
    fun `creating with no monitored apps surfaces the use case's blacklist error`() {
        val viewModel = viewModel(
            JoinCommunityFakeRepository(),
            monitoredAppsRepository = JoinCommunityFakeMonitoredAppsRepository(emptySet())
        )

        viewModel.onCreateClicked()

        assertEquals("Add at least one app to monitor first", viewModel.uiState.value.createError)
        assertNull(viewModel.uiState.value.createdInviteCode)
    }
}
