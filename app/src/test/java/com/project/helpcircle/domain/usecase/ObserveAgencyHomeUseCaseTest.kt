package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.CrisisEpisodeRecord
import com.project.helpcircle.domain.model.FocusSession
import com.project.helpcircle.domain.model.WeeklySummary
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.WeeklyHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class HomeFakeAgencyRepository(initialIndex: AgencyIndex) : AgencyRepository {
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

private class HomeFakeWeeklyHistoryRepository(initialSummaries: List<WeeklySummary>) : WeeklyHistoryRepository {
    private val summariesFlow = MutableStateFlow(initialSummaries)
    override suspend fun recordCrisisEpisode(record: CrisisEpisodeRecord) = Unit
    override suspend fun getCrisisEpisodesSince(sinceEpochMillis: Long): List<CrisisEpisodeRecord> = emptyList()
    override suspend fun saveWeeklySummary(summary: WeeklySummary) {
        summariesFlow.value = summariesFlow.value + summary
    }
    override val weeklySummaries: Flow<List<WeeklySummary>> = summariesFlow
}

class ObserveAgencyHomeUseCaseTest {

    @Test
    fun `with no weekly history the latest summary is null`() = runBlocking {
        val useCase = ObserveAgencyHomeUseCase(
            HomeFakeAgencyRepository(AgencyIndex.of(72)),
            HomeFakeWeeklyHistoryRepository(emptyList())
        )

        val summary = useCase().first()

        assertEquals(72, summary.currentIndex.value)
        assertNull(summary.latestWeeklySummary)
    }

    @Test
    fun `weekly summaries are sorted oldest first regardless of input order`() = runBlocking {
        val oldest = WeeklySummary(weekStartEpochMillis = 1_000, agencyIndexDelta = 5, mostEffectiveInterventionCategory = "Text", peakCrisisHour = 22)
        val newest = WeeklySummary(weekStartEpochMillis = 2_000, agencyIndexDelta = -3, mostEffectiveInterventionCategory = "Haptic", peakCrisisHour = 23)
        val useCase = ObserveAgencyHomeUseCase(
            HomeFakeAgencyRepository(AgencyIndex.baseline()),
            HomeFakeWeeklyHistoryRepository(listOf(newest, oldest))
        )

        val summary = useCase().first()

        assertEquals(listOf(oldest, newest), summary.weeklySummariesOldestFirst)
        assertEquals(newest, summary.latestWeeklySummary)
    }
}
