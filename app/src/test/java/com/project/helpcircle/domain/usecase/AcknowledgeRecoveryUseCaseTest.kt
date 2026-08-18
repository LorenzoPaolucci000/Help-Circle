package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.CrisisEpisodeRecord
import com.project.helpcircle.domain.model.FocusSession
import com.project.helpcircle.domain.model.WeeklySummary
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.WeeklyHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class AcknowledgeRecoveryFakeAgencyRepository : AgencyRepository {
    val indexFlow = MutableStateFlow(AgencyIndex.baseline())
    val stateFlow = MutableStateFlow<AgencyState>(AgencyState.Stable)
    private var deltaAutonomy = 0
    private var deltaSupport = 0

    override val currentAgencyIndex: Flow<AgencyIndex> = indexFlow
    override val currentAgencyState: Flow<AgencyState> = stateFlow

    override suspend fun recordFocusSession(session: FocusSession) = Unit
    override suspend fun updateAgencyIndex(index: AgencyIndex) {
        indexFlow.value = index
    }
    override suspend fun reportAgencyState(state: AgencyState) {
        stateFlow.value = state
    }
    override suspend fun adjustAgencyDeltas(deltaAutonomy: Int, deltaSupport: Int): AgencyIndex {
        this.deltaAutonomy += deltaAutonomy
        this.deltaSupport += deltaSupport
        val index = AgencyIndex.calculate(this.deltaAutonomy, this.deltaSupport)
        indexFlow.value = index
        return index
    }
    override suspend fun getLastArchivedAgencyIndex(): Int? = null
    override suspend fun archiveAgencyIndex(agencyIndexValue: Int) = Unit
    override suspend fun getLastWeeklyResetAtEpochMillis(): Long? = null
    override suspend fun resetAgencyIndexForNewWeek(atEpochMillis: Long) = Unit
}

private class AcknowledgeRecoveryFakeWeeklyHistoryRepository : WeeklyHistoryRepository {
    val recordedEpisodes = mutableListOf<CrisisEpisodeRecord>()
    private val summariesFlow = MutableStateFlow<List<WeeklySummary>>(emptyList())

    override suspend fun recordCrisisEpisode(record: CrisisEpisodeRecord) {
        recordedEpisodes += record
    }
    override suspend fun getCrisisEpisodesBetween(
        fromEpochMillis: Long,
        untilEpochMillis: Long
    ): List<CrisisEpisodeRecord> = recordedEpisodes.filter {
        it.startedAtEpochMillis >= fromEpochMillis && it.startedAtEpochMillis < untilEpochMillis
    }
    override suspend fun deleteCrisisEpisodesBefore(beforeEpochMillis: Long) {
        recordedEpisodes.removeAll { it.startedAtEpochMillis < beforeEpochMillis }
    }
    override suspend fun saveWeeklySummary(summary: WeeklySummary) {
        summariesFlow.update { it + summary }
    }
    override val weeklySummaries: Flow<List<WeeklySummary>> = summariesFlow
}

private fun useCase(
    agencyRepository: AcknowledgeRecoveryFakeAgencyRepository,
    weeklyHistoryRepository: AcknowledgeRecoveryFakeWeeklyHistoryRepository,
    tracker: CrisisEpisodeTracker
) = AcknowledgeRecoveryUseCase(
    agencyRepository,
    tracker,
    CalculateAgencyIndexUseCase(agencyRepository),
    weeklyHistoryRepository
)

class AcknowledgeRecoveryUseCaseTest {

    @Test
    fun `acknowledging recovery with no crisis open reports Stable but earns nothing`() = runBlocking {
        val agencyRepository = AcknowledgeRecoveryFakeAgencyRepository()
        val weeklyHistoryRepository = AcknowledgeRecoveryFakeWeeklyHistoryRepository()

        useCase(agencyRepository, weeklyHistoryRepository, CrisisEpisodeTracker())(atEpochMillis = 1_000)

        assertEquals(AgencyState.Stable, agencyRepository.stateFlow.value)
        assertEquals(AgencyIndex.baseline(), agencyRepository.indexFlow.value)
        assertTrue(weeklyHistoryRepository.recordedEpisodes.isEmpty())
    }

    @Test
    fun `acknowledging recovery within the spontaneous window closes the episode and applies Delta_Autonomy`() = runBlocking {
        val agencyRepository = AcknowledgeRecoveryFakeAgencyRepository()
        val weeklyHistoryRepository = AcknowledgeRecoveryFakeWeeklyHistoryRepository()
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)

        useCase(agencyRepository, weeklyHistoryRepository, tracker)(atEpochMillis = 30_000)

        assertEquals(55, agencyRepository.indexFlow.value.value)
        assertEquals(1, weeklyHistoryRepository.recordedEpisodes.size)
        val closed = weeklyHistoryRepository.recordedEpisodes.single()
        assertEquals(0L, closed.startedAtEpochMillis)
        assertEquals(false, closed.wasEffectiveIntervention)
    }

    @Test
    fun `acknowledging recovery after an effective nudge applies Delta_Support and records its category`() = runBlocking {
        val agencyRepository = AcknowledgeRecoveryFakeAgencyRepository()
        val weeklyHistoryRepository = AcknowledgeRecoveryFakeWeeklyHistoryRepository()
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        tracker.onNudgeReceived(atEpochMillis = 5_000, category = "Text")

        useCase(agencyRepository, weeklyHistoryRepository, tracker)(atEpochMillis = 95_000)

        assertEquals(60, agencyRepository.indexFlow.value.value)
        val closed = weeklyHistoryRepository.recordedEpisodes.single()
        assertEquals("Text", closed.nudgeCategory)
        assertEquals(true, closed.wasEffectiveIntervention)
    }
}
