package com.project.helpcircle.presentation.home

import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.CrisisEpisodeRecord
import com.project.helpcircle.domain.model.FocusSession
import com.project.helpcircle.domain.model.WeeklySummary
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.WeeklyHistoryRepository
import com.project.helpcircle.domain.usecase.ObserveAgencyHomeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class HomeViewModelFakeAgencyRepository(initialIndex: AgencyIndex) : AgencyRepository {
    val indexFlow = MutableStateFlow(initialIndex)
    override val currentAgencyIndex: Flow<AgencyIndex> = indexFlow
    override val currentAgencyState: Flow<AgencyState> = MutableStateFlow(AgencyState.Stable)

    override suspend fun recordFocusSession(session: FocusSession) = Unit
    override suspend fun updateAgencyIndex(index: AgencyIndex) {
        indexFlow.value = index
    }
    override suspend fun reportAgencyState(state: AgencyState) = Unit
    override suspend fun adjustAgencyDeltas(deltaAutonomy: Int, deltaSupport: Int): AgencyIndex = indexFlow.value
    override suspend fun getLastArchivedAgencyIndex(): Int? = null
    override suspend fun archiveAgencyIndex(agencyIndexValue: Int) = Unit
    override suspend fun getLastWeeklyResetAtEpochMillis(): Long? = null
    override suspend fun resetAgencyIndexForNewWeek(atEpochMillis: Long) = Unit
}

private class HomeViewModelFakeWeeklyHistoryRepository(initialSummaries: List<WeeklySummary>) : WeeklyHistoryRepository {
    override suspend fun recordCrisisEpisode(record: CrisisEpisodeRecord) = Unit
    override suspend fun getCrisisEpisodesSince(sinceEpochMillis: Long): List<CrisisEpisodeRecord> = emptyList()
    override suspend fun saveWeeklySummary(summary: WeeklySummary) = Unit
    override val weeklySummaries: Flow<List<WeeklySummary>> = MutableStateFlow(initialSummaries)
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `with no weekly history the ui state shows the live index and no summary`() {
        val useCase = ObserveAgencyHomeUseCase(
            HomeViewModelFakeAgencyRepository(AgencyIndex.of(65)),
            HomeViewModelFakeWeeklyHistoryRepository(emptyList())
        )

        val viewModel = HomeViewModel(useCase)

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(65, viewModel.uiState.value.currentAgencyIndex)
        assertEquals(emptyList<Int>(), viewModel.uiState.value.weeklyDeltasOldestFirst)
        assertNull(viewModel.uiState.value.latestWeeklySummary)
        assertNull(viewModel.uiState.value.previousWeeklySummary)
    }

    @Test
    fun `weekly deltas and the latest summary are surfaced from the use case`() {
        val oldest = WeeklySummary(weekStartEpochMillis = 1_000, agencyIndexDelta = 5, mostEffectiveInterventionCategory = "Text", peakCrisisHour = 22)
        val newest = WeeklySummary(weekStartEpochMillis = 2_000, agencyIndexDelta = -3, mostEffectiveInterventionCategory = "Haptic", peakCrisisHour = 23)
        val useCase = ObserveAgencyHomeUseCase(
            HomeViewModelFakeAgencyRepository(AgencyIndex.baseline()),
            HomeViewModelFakeWeeklyHistoryRepository(listOf(newest, oldest))
        )

        val viewModel = HomeViewModel(useCase)

        assertEquals(listOf(5, -3), viewModel.uiState.value.weeklyDeltasOldestFirst)
        assertEquals(newest, viewModel.uiState.value.latestWeeklySummary)
        assertEquals(oldest, viewModel.uiState.value.previousWeeklySummary)
    }

    @Test
    fun `previous summary is the second-most-recent week, not merely the oldest`() {
        val oldest = WeeklySummary(weekStartEpochMillis = 1_000, agencyIndexDelta = 5, mostEffectiveInterventionCategory = "Text", peakCrisisHour = 22)
        val middle = WeeklySummary(weekStartEpochMillis = 2_000, agencyIndexDelta = 2, mostEffectiveInterventionCategory = "Haptic", peakCrisisHour = 21)
        val newest = WeeklySummary(weekStartEpochMillis = 3_000, agencyIndexDelta = -3, mostEffectiveInterventionCategory = "Blur", peakCrisisHour = 23)
        val useCase = ObserveAgencyHomeUseCase(
            HomeViewModelFakeAgencyRepository(AgencyIndex.baseline()),
            HomeViewModelFakeWeeklyHistoryRepository(listOf(newest, oldest, middle))
        )

        val viewModel = HomeViewModel(useCase)

        assertEquals(newest, viewModel.uiState.value.latestWeeklySummary)
        assertEquals(middle, viewModel.uiState.value.previousWeeklySummary)
    }
}
