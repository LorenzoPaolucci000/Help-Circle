package com.project.helpcircle.presentation.settings

import com.project.helpcircle.domain.model.AppCategory
import com.project.helpcircle.domain.model.AppInfo
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.InstalledAppsRepository
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import com.project.helpcircle.domain.usecase.GetInstalledAppsUseCase
import com.project.helpcircle.domain.usecase.LeaveCommunityUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class SettingsFakeInstalledAppsRepository(private val apps: List<AppInfo>) : InstalledAppsRepository {
    override suspend fun getInstalledApps(): List<AppInfo> = apps
}

private class SettingsFakeMonitoredAppsRepository(initial: Set<String>) : MonitoredAppsRepository {
    private val flow = MutableStateFlow(initial)
    val setMonitoredCalls = mutableListOf<Pair<String, Boolean>>()

    override val monitoredPackageNames: Flow<Set<String>> = flow
    override suspend fun setMonitored(packageName: String, isMonitored: Boolean) {
        setMonitoredCalls += packageName to isMonitored
    }
    override suspend fun isMonitored(packageName: String): Boolean = packageName in flow.value
}

private class SettingsFakeCommunityRepository(private val activeCommunityId: String?) : CommunityRepository {
    var leftCommunityId: String? = null

    override fun observeCommunityState(communityId: String): Flow<CommunityState> = emptyFlow()
    override suspend fun joinCommunity(communityId: String): CommunityState = throw UnsupportedOperationException()
    override suspend fun createCommunity(inviteCode: String): CommunityState = throw UnsupportedOperationException()
    override suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState? = null
    override suspend fun reportCrisis(communityId: String) = Unit
    override suspend fun reportRecovery(communityId: String) = Unit
    override suspend fun leaveCommunity(communityId: String) {
        leftCommunityId = communityId
    }
    override suspend fun getActiveCommunityId(): String? = activeCommunityId
    override suspend fun getMemberCount(communityId: String): Int = 0
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val socialApp = AppInfo("com.social", "SocialApp", AppCategory.SOCIAL)
    private val newsApp = AppInfo("com.news", "NewsApp", AppCategory.NEWS)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        apps: List<AppInfo> = listOf(socialApp, newsApp),
        monitored: Set<String> = emptySet(),
        activeCommunityId: String? = "comm-1",
        communityRepository: SettingsFakeCommunityRepository = SettingsFakeCommunityRepository(activeCommunityId)
    ): Pair<SettingsViewModel, SettingsFakeMonitoredAppsRepository> {
        val monitoredAppsRepository = SettingsFakeMonitoredAppsRepository(monitored)
        val viewModel = SettingsViewModel(
            GetInstalledAppsUseCase(SettingsFakeInstalledAppsRepository(apps)),
            monitoredAppsRepository,
            communityRepository,
            LeaveCommunityUseCase(communityRepository)
        )
        return viewModel to monitoredAppsRepository
    }

    @Test
    fun `loads installed apps and the saved blacklist on init`() {
        val (viewModel, _) = viewModel(monitored = setOf("com.social"))

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(newsApp, socialApp), state.apps)
        assertEquals(setOf("com.social"), state.pendingMonitoredPackageNames)
    }

    @Test
    fun `search query filters apps by category grouping`() {
        val (viewModel, _) = viewModel()

        viewModel.onSearchQueryChanged("news")

        assertEquals(mapOf(AppCategory.NEWS to listOf(newsApp)), viewModel.uiState.value.appsByCategory)
    }

    @Test
    fun `toggling an app flips its pending blacklist membership without saving yet`() {
        val (viewModel, monitoredAppsRepository) = viewModel()

        viewModel.onAppToggled("com.social")

        assertTrue("com.social" in viewModel.uiState.value.pendingMonitoredPackageNames)
        assertTrue(monitoredAppsRepository.setMonitoredCalls.isEmpty())
    }

    @Test
    fun `saving only persists apps whose blacklist membership actually changed`() {
        val (viewModel, monitoredAppsRepository) = viewModel(monitored = setOf("com.news"))
        viewModel.onAppToggled("com.social")
        viewModel.onAppToggled("com.news")

        viewModel.onSaveClicked()

        assertEquals(
            listOf("com.social" to true, "com.news" to false),
            monitoredAppsRepository.setMonitoredCalls
        )
        assertEquals("Configuration saved", viewModel.uiState.value.saveMessage)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `leave community shows a confirmation before doing anything`() {
        val (viewModel, _) = viewModel()

        viewModel.onLeaveCommunityClicked()

        assertTrue(viewModel.uiState.value.showLeaveConfirmation)
    }

    @Test
    fun `dismissing the leave confirmation cancels without leaving`() {
        val communityRepository = SettingsFakeCommunityRepository(activeCommunityId = "comm-1")
        val (viewModel, _) = viewModel(activeCommunityId = "comm-1", communityRepository = communityRepository)
        viewModel.onLeaveCommunityClicked()

        viewModel.onLeaveCommunityDismissed()

        assertFalse(viewModel.uiState.value.showLeaveConfirmation)
        assertEquals(null, communityRepository.leftCommunityId)
    }

    @Test
    fun `confirming leave community calls the use case with the active community and signals completion`() {
        val communityRepository = SettingsFakeCommunityRepository(activeCommunityId = "comm-1")
        val (viewModel, _) = viewModel(activeCommunityId = "comm-1", communityRepository = communityRepository)
        viewModel.onLeaveCommunityClicked()

        viewModel.onLeaveCommunityConfirmed()

        assertEquals("comm-1", communityRepository.leftCommunityId)
        assertTrue(viewModel.uiState.value.hasLeftCommunity)
        assertFalse(viewModel.uiState.value.showLeaveConfirmation)
    }

    @Test
    fun `handling the left-community event clears the one-shot flag`() {
        val (viewModel, _) = viewModel()
        viewModel.onLeaveCommunityConfirmed()

        viewModel.onLeftCommunityHandled()

        assertFalse(viewModel.uiState.value.hasLeftCommunity)
    }
}
