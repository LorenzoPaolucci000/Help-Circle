package com.project.helpcircle.presentation.onboarding

import com.project.helpcircle.domain.model.AppCategory
import com.project.helpcircle.domain.model.AppInfo
import com.project.helpcircle.domain.repository.InstalledAppsRepository
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import com.project.helpcircle.domain.usecase.GetInstalledAppsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class SelectAppsFakeInstalledAppsRepository(private val apps: List<AppInfo>) : InstalledAppsRepository {
    override suspend fun getInstalledApps(): List<AppInfo> = apps
}

private class SelectAppsFakeMonitoredAppsRepository(initial: Set<String> = emptySet()) : MonitoredAppsRepository {
    private val flow = MutableStateFlow(initial)
    val setMonitoredCalls = mutableListOf<Pair<String, Boolean>>()

    override val monitoredPackageNames: Flow<Set<String>> = flow
    override suspend fun setMonitored(packageName: String, isMonitored: Boolean) {
        setMonitoredCalls += packageName to isMonitored
    }
    override suspend fun isMonitored(packageName: String): Boolean = packageName in flow.value
}

@OptIn(ExperimentalCoroutinesApi::class)
class SelectMonitoredAppsViewModelTest {

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
        monitoredAppsRepository: SelectAppsFakeMonitoredAppsRepository = SelectAppsFakeMonitoredAppsRepository()
    ) = SelectMonitoredAppsViewModel(
        GetInstalledAppsUseCase(SelectAppsFakeInstalledAppsRepository(apps)),
        monitoredAppsRepository
    )

    @Test
    fun `loads installed apps on init with nothing pending yet`() {
        val viewModel = viewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(newsApp, socialApp), state.apps)
        assertTrue(state.pendingMonitoredPackageNames.isEmpty())
    }

    @Test
    fun `continuing with nothing selected shows the required-apps error and does not save`() {
        val monitoredAppsRepository = SelectAppsFakeMonitoredAppsRepository()
        val viewModel = viewModel(monitoredAppsRepository = monitoredAppsRepository)

        viewModel.onContinueClicked()

        assertTrue(viewModel.uiState.value.showEmptySelectionError)
        assertFalse(viewModel.uiState.value.isDone)
        assertTrue(monitoredAppsRepository.setMonitoredCalls.isEmpty())
    }

    @Test
    fun `toggling an app clears any prior required-apps error`() {
        val viewModel = viewModel()
        viewModel.onContinueClicked()

        viewModel.onAppToggled("com.social")

        assertFalse(viewModel.uiState.value.showEmptySelectionError)
    }

    @Test
    fun `continuing with at least one app selected saves it and signals done`() {
        val monitoredAppsRepository = SelectAppsFakeMonitoredAppsRepository()
        val viewModel = viewModel(monitoredAppsRepository = monitoredAppsRepository)
        viewModel.onAppToggled("com.social")

        viewModel.onContinueClicked()

        assertEquals(listOf("com.social" to true), monitoredAppsRepository.setMonitoredCalls)
        assertTrue(viewModel.uiState.value.isDone)
        assertFalse(viewModel.uiState.value.isSaving)
    }
}
