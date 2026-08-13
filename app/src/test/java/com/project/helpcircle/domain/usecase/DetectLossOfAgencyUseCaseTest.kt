package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.AgencyDetectionEngine
import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.engine.ScrollSignal
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
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class DetectLossFakeAgencyRepository : AgencyRepository {
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

private class DetectLossFakeWeeklyHistoryRepository : WeeklyHistoryRepository {
    val recordedEpisodes = mutableListOf<CrisisEpisodeRecord>()
    private val summariesFlow = MutableStateFlow<List<WeeklySummary>>(emptyList())

    override suspend fun recordCrisisEpisode(record: CrisisEpisodeRecord) {
        recordedEpisodes += record
    }
    override suspend fun getCrisisEpisodesSince(sinceEpochMillis: Long): List<CrisisEpisodeRecord> =
        recordedEpisodes.filter { it.startedAtEpochMillis >= sinceEpochMillis }
    override suspend fun saveWeeklySummary(summary: WeeklySummary) {
        summariesFlow.update { it + summary }
    }
    override val weeklySummaries: Flow<List<WeeklySummary>> = summariesFlow
}

private fun useCase(
    engine: AgencyDetectionEngine,
    agencyRepository: DetectLossFakeAgencyRepository,
    weeklyHistoryRepository: DetectLossFakeWeeklyHistoryRepository,
    tracker: CrisisEpisodeTracker = CrisisEpisodeTracker()
) = DetectLossOfAgencyUseCase(
    engine,
    agencyRepository,
    tracker,
    CalculateAgencyIndexUseCase(agencyRepository),
    weeklyHistoryRepository
)

class DetectLossOfAgencyUseCaseTest {

    @Test
    fun `staying stable reports Stable and earns no delta`() = runBlocking {
        val agencyRepository = DetectLossFakeAgencyRepository()
        val weeklyHistoryRepository = DetectLossFakeWeeklyHistoryRepository()
        val engine = AgencyDetectionEngine(scrollThreshold = 100, warningRatio = 0.6)

        val state = useCase(engine, agencyRepository, weeklyHistoryRepository)(ScrollSignal(0))

        assertEquals(AgencyState.Stable, state)
        assertEquals(AgencyState.Stable, agencyRepository.stateFlow.value)
        assertEquals(AgencyIndex.baseline(), agencyRepository.indexFlow.value)
        assertTrue(weeklyHistoryRepository.recordedEpisodes.isEmpty())
    }

    @Test
    fun `entering a crisis reports Crisis but earns no delta yet`() = runBlocking {
        val agencyRepository = DetectLossFakeAgencyRepository()
        val weeklyHistoryRepository = DetectLossFakeWeeklyHistoryRepository()
        val engine = AgencyDetectionEngine(scrollThreshold = 1, warningRatio = 0.6)

        val state = useCase(engine, agencyRepository, weeklyHistoryRepository)(ScrollSignal(0))

        assertEquals(AgencyState.Crisis, state)
        assertEquals(AgencyState.Crisis, agencyRepository.stateFlow.value)
        assertEquals(AgencyIndex.baseline(), agencyRepository.indexFlow.value)
        assertTrue(weeklyHistoryRepository.recordedEpisodes.isEmpty())
    }

    @Test
    fun `a spontaneous recovery applies Delta_Autonomy and archives the closed episode`() = runBlocking {
        val agencyRepository = DetectLossFakeAgencyRepository()
        val weeklyHistoryRepository = DetectLossFakeWeeklyHistoryRepository()
        // scrollThreshold=2 and warningThreshold=2 means a single isolated signal (after the
        // sliding window evicts the earlier ones) is neither Warning nor Crisis, so Stable is
        // reachable again after a crisis — mirrors AgencyDetectionEngineTest's own eviction test.
        val engine = AgencyDetectionEngine(windowSize = 1.seconds, scrollThreshold = 2, warningRatio = 1.0)
        val detectLossOfAgency = useCase(engine, agencyRepository, weeklyHistoryRepository)

        detectLossOfAgency(ScrollSignal(0))
        val crisisState = detectLossOfAgency(ScrollSignal(100))
        val recoveredState = detectLossOfAgency(ScrollSignal(2_100))

        assertEquals(AgencyState.Crisis, crisisState)
        assertEquals(AgencyState.Stable, recoveredState)
        assertEquals(55, agencyRepository.indexFlow.value.value)
        assertEquals(1, weeklyHistoryRepository.recordedEpisodes.size)
        assertEquals(100L, weeklyHistoryRepository.recordedEpisodes.single().startedAtEpochMillis)
    }
}
