package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.AgencyDetectionEngine
import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.engine.DetectionConfig
import com.project.helpcircle.domain.engine.ScrollSignal
import com.project.helpcircle.domain.engine.SystemFallbackEvaluator
import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.CrisisEpisodeRecord
import com.project.helpcircle.domain.model.FocusSession
import com.project.helpcircle.domain.model.WeeklySummary
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.WeeklyHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

private class DetectLossFakeCommunityRepository(
    private val activeCommunityId: String?,
    private val memberCount: Int
) : CommunityRepository {
    override fun observeCommunityState(communityId: String): Flow<CommunityState> =
        MutableStateFlow(CommunityState(communityId, emptyList(), cohesionBonusApplied = false))
    override suspend fun joinCommunity(communityId: String): CommunityState = throw UnsupportedOperationException()
    override suspend fun createCommunity(communityId: String, inviteCode: String, name: String): CommunityState = throw UnsupportedOperationException()
    override suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState? = null
    override suspend fun reportCrisis(communityId: String) = Unit
    override suspend fun reportRecovery(communityId: String) = Unit
    override suspend fun publishSatisfaction(
        communityId: String,
        weekStartEpochMillis: Long,
        satisfaction: WeeklySatisfaction
    ) = Unit
    override suspend fun leaveCommunity(communityId: String) = Unit
    override suspend fun getActiveCommunityId(): String? = activeCommunityId
    override suspend fun getMemberCount(communityId: String): Int = memberCount
}

private fun useCase(
    engine: AgencyDetectionEngine,
    agencyRepository: DetectLossFakeAgencyRepository,
    weeklyHistoryRepository: DetectLossFakeWeeklyHistoryRepository,
    tracker: CrisisEpisodeTracker = CrisisEpisodeTracker(),
    communityRepository: CommunityRepository = DetectLossFakeCommunityRepository(activeCommunityId = "comm-1", memberCount = 2)
): DetectLossOfAgencyUseCase {
    val calculateAgencyIndexUseCase = CalculateAgencyIndexUseCase(agencyRepository)
    return DetectLossOfAgencyUseCase(
        engine,
        agencyRepository,
        tracker,
        calculateAgencyIndexUseCase,
        weeklyHistoryRepository,
        EvaluateSystemFallbackUseCase(tracker, SystemFallbackEvaluator(tracker), communityRepository),
        AcknowledgeRecoveryUseCase(agencyRepository, tracker, calculateAgencyIndexUseCase, weeklyHistoryRepository),
        communityRepository
    )
}

class DetectLossOfAgencyUseCaseTest {

    @Test
    fun `staying stable reports Stable and earns no delta`() = runBlocking {
        val agencyRepository = DetectLossFakeAgencyRepository()
        val weeklyHistoryRepository = DetectLossFakeWeeklyHistoryRepository()
        val engine = AgencyDetectionEngine(scrollThreshold = 100, warningRatio = 0.6)

        val result = useCase(engine, agencyRepository, weeklyHistoryRepository)(ScrollSignal(0))

        assertEquals(AgencyState.Stable, result.state)
        assertFalse(result.offerSystemFallback)
        assertEquals(AgencyState.Stable, agencyRepository.stateFlow.value)
        assertEquals(AgencyIndex.baseline(), agencyRepository.indexFlow.value)
        assertTrue(weeklyHistoryRepository.recordedEpisodes.isEmpty())
    }

    @Test
    fun `entering a crisis reports Crisis but earns no delta yet`() = runBlocking {
        val agencyRepository = DetectLossFakeAgencyRepository()
        val weeklyHistoryRepository = DetectLossFakeWeeklyHistoryRepository()
        val engine = AgencyDetectionEngine(scrollThreshold = 1, warningRatio = 0.6)

        val result = useCase(engine, agencyRepository, weeklyHistoryRepository)(ScrollSignal(0))

        assertEquals(AgencyState.Crisis, result.state)
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
        val crisisResult = detectLossOfAgency(ScrollSignal(100))
        val recoveredResult = detectLossOfAgency(ScrollSignal(2_100))

        assertEquals(AgencyState.Crisis, crisisResult.state)
        assertEquals(AgencyState.Stable, recoveredResult.state)
        assertEquals(55, agencyRepository.indexFlow.value.value)
        assertEquals(1, weeklyHistoryRepository.recordedEpisodes.size)
        assertEquals(100L, weeklyHistoryRepository.recordedEpisodes.single().startedAtEpochMillis)
    }

    @Test
    fun `a crisis in a solo community offers the system fallback immediately`() = runBlocking {
        val agencyRepository = DetectLossFakeAgencyRepository()
        val weeklyHistoryRepository = DetectLossFakeWeeklyHistoryRepository()
        val engine = AgencyDetectionEngine(scrollThreshold = 1, warningRatio = 0.6)
        val communityRepository = DetectLossFakeCommunityRepository(activeCommunityId = "comm-1", memberCount = 1)

        val result = useCase(
            engine,
            agencyRepository,
            weeklyHistoryRepository,
            communityRepository = communityRepository
        )(ScrollSignal(0))

        assertEquals(AgencyState.Crisis, result.state)
        assertTrue(result.offerSystemFallback)
    }

    @Test
    fun `a crisis in a populated community does not offer the fallback right away`() = runBlocking {
        val agencyRepository = DetectLossFakeAgencyRepository()
        val weeklyHistoryRepository = DetectLossFakeWeeklyHistoryRepository()
        val engine = AgencyDetectionEngine(scrollThreshold = 1, warningRatio = 0.6)

        val result = useCase(engine, agencyRepository, weeklyHistoryRepository)(ScrollSignal(0))

        assertEquals(AgencyState.Crisis, result.state)
        assertFalse(result.offerSystemFallback)
    }

    @Test
    fun `resuming scrolling before the break duration elapses cancels it and awards nothing`() = runBlocking {
        val agencyRepository = DetectLossFakeAgencyRepository()
        val weeklyHistoryRepository = DetectLossFakeWeeklyHistoryRepository()
        val engine = AgencyDetectionEngine(windowSize = 200.seconds, scrollThreshold = 2, warningRatio = 1.0)
        val tracker = CrisisEpisodeTracker()
        val detectLossOfAgency = useCase(engine, agencyRepository, weeklyHistoryRepository, tracker)
        detectLossOfAgency(ScrollSignal(0))
        detectLossOfAgency(ScrollSignal(100))
        tracker.onBreakStarted(atEpochMillis = 100)

        // Comes back and scrolls again well before the 2-minute break duration is up.
        val earlyResult = detectLossOfAgency(ScrollSignal(100 + 10_000))

        assertEquals(AgencyIndex.baseline(), agencyRepository.indexFlow.value)
        assertNull(tracker.pendingBreakStartedAtMillis())
        assertTrue(weeklyHistoryRepository.recordedEpisodes.isEmpty())
        assertEquals(AgencyState.Crisis, earlyResult.state)
    }

    @Test
    fun `a genuinely completed break awards the assisted-break bonus and resets the sliding window`() = runBlocking {
        val agencyRepository = DetectLossFakeAgencyRepository()
        val weeklyHistoryRepository = DetectLossFakeWeeklyHistoryRepository()
        // A window wider than the break duration proves the reset actually matters: without it,
        // the two crisis-triggering signals from before the break would still be inside this
        // window by the time the break resolves, and would immediately push state back to Crisis.
        val engine = AgencyDetectionEngine(windowSize = 200.seconds, scrollThreshold = 2, warningRatio = 1.0)
        val tracker = CrisisEpisodeTracker()
        val detectLossOfAgency = useCase(engine, agencyRepository, weeklyHistoryRepository, tracker)
        detectLossOfAgency(ScrollSignal(0))
        val crisisResult = detectLossOfAgency(ScrollSignal(100))
        tracker.onBreakStarted(atEpochMillis = 100)

        val resolvedResult = detectLossOfAgency(
            ScrollSignal(100 + DetectionConfig.SYSTEM_FALLBACK_BREAK_DURATION_MS)
        )

        assertEquals(AgencyState.Crisis, crisisResult.state)
        assertEquals(AgencyState.Stable, resolvedResult.state)
        assertEquals(50 + DetectionConfig.ASSISTED_BREAK_COMPLETION_DELTA, agencyRepository.indexFlow.value.value)
        assertNull(tracker.pendingBreakStartedAtMillis())
        assertEquals(1, weeklyHistoryRepository.recordedEpisodes.size)
    }

    @Test
    fun `organic spontaneous recovery unrelated to a break is unaffected and still awards plus 5`() = runBlocking {
        val agencyRepository = DetectLossFakeAgencyRepository()
        val weeklyHistoryRepository = DetectLossFakeWeeklyHistoryRepository()
        val engine = AgencyDetectionEngine(windowSize = 1.seconds, scrollThreshold = 2, warningRatio = 1.0)
        val detectLossOfAgency = useCase(engine, agencyRepository, weeklyHistoryRepository)

        detectLossOfAgency(ScrollSignal(0))
        detectLossOfAgency(ScrollSignal(100))
        val recoveredResult = detectLossOfAgency(ScrollSignal(2_100))

        assertEquals(AgencyState.Stable, recoveredResult.state)
        assertEquals(50 + DetectionConfig.SPONTANEOUS_RECOVERY_DELTA, agencyRepository.indexFlow.value.value)
    }
}
