package com.project.helpcircle.presentation.common

import com.project.helpcircle.domain.repository.MonitoringStatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeMonitoringStatusRepository(initiallyActive: Boolean) : MonitoringStatusRepository {
    val activeFlow = MutableStateFlow(initiallyActive)
    private var nowValue = initiallyActive

    override val isMonitoringActive: Flow<Boolean> = activeFlow
    override fun isMonitoringActiveNow(): Boolean = nowValue

    fun emit(isActive: Boolean) {
        nowValue = isActive
        activeFlow.value = isActive
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MonitoringStatusViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `seeds from the synchronous check so a cold start does not flash the banner`() {
        val repository = FakeMonitoringStatusRepository(initiallyActive = true)

        val viewModel = MonitoringStatusViewModel(repository)

        assertTrue(viewModel.isMonitoringActive.value)
    }

    @Test
    fun `reports monitoring as off when the permission is revoked while the app is running`() {
        // The real-device failure mode: the OS clears the permission underneath a live process.
        val repository = FakeMonitoringStatusRepository(initiallyActive = true)
        val viewModel = MonitoringStatusViewModel(repository)

        repository.emit(isActive = false)

        assertFalse(viewModel.isMonitoringActive.value)
    }

    @Test
    fun `clears the warning once monitoring is turned back on`() {
        val repository = FakeMonitoringStatusRepository(initiallyActive = false)
        val viewModel = MonitoringStatusViewModel(repository)
        assertFalse(viewModel.isMonitoringActive.value)

        repository.emit(isActive = true)

        assertTrue(viewModel.isMonitoringActive.value)
    }
}
