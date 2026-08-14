package com.project.helpcircle.presentation.fallback

import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.usecase.StartSystemFallbackBreakUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SystemFallbackViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(tracker: CrisisEpisodeTracker = CrisisEpisodeTracker()) =
        SystemFallbackViewModel(StartSystemFallbackBreakUseCase(tracker))

    @Test
    fun `taking the break records it as started via the use case but does not score or dismiss immediately`() = runTest(testDispatcher) {
        // Regression test for the scoring exploit: tapping the button used to score instantly.
        val tracker = CrisisEpisodeTracker()
        val viewModel = viewModel(tracker)

        viewModel.onTakeBreakClicked()

        assertTrue(viewModel.uiState.value.isBreakStarted)
        assertFalse(viewModel.uiState.value.isDismissed)
        assertNotNull(tracker.pendingBreakStartedAtMillis())
    }

    @Test
    fun `the prompt dismisses itself once the confirmation has been shown`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onTakeBreakClicked()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDismissed)
    }

    @Test
    fun `continuing dismisses immediately without starting a break`() = runTest(testDispatcher) {
        val tracker = CrisisEpisodeTracker()
        val viewModel = viewModel(tracker)

        viewModel.onContinueClicked()

        assertTrue(viewModel.uiState.value.isDismissed)
        assertFalse(viewModel.uiState.value.isBreakStarted)
        assertNull(tracker.pendingBreakStartedAtMillis())
    }
}
