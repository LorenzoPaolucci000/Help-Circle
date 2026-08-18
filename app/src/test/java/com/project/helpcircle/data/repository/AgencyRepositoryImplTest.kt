package com.project.helpcircle.data.repository

import com.project.helpcircle.data.local.dao.AgencyStateDao
import com.project.helpcircle.data.local.dao.FocusSessionDao
import com.project.helpcircle.data.local.entity.AgencyStateEntity
import com.project.helpcircle.data.local.entity.FocusSessionEntity
import com.project.helpcircle.domain.engine.WeeklyResetCalculator
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.CrisisEpisodeRecord
import com.project.helpcircle.domain.model.WeeklySummary
import com.project.helpcircle.domain.repository.WeeklyHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [AgencyRepositoryImpl.reportAgencyState], which sits on the accessibility service's
 * per-scroll path and therefore must not write once per scroll. The counters below exist to assert
 * the *absence* of writes, which is the whole point: a plain state assertion would pass just as
 * happily against a version that rewrites an identical row for every event.
 *
 * The weekly-reset wiring on the same class still isn't covered here — it needs an in-memory Room
 * instance rather than a fake DAO.
 */
private class FakeAgencyStateDao : AgencyStateDao {
    var stored: AgencyStateEntity? = null
    var upsertCount = 0

    /**
     * Counts *collections*, not calls: Room's observable queries are cold, so obtaining the flow
     * registers nothing and only subscribing to it sets up an invalidation observer. Counting calls
     * would misreport, since the repository obtains this flow once in a property initializer.
     */
    var subscriptionCount = 0

    override fun observe(): Flow<AgencyStateEntity?> = flow {
        subscriptionCount++
        emit(stored)
    }

    override suspend fun get(): AgencyStateEntity? = stored

    override suspend fun upsert(entity: AgencyStateEntity) {
        upsertCount++
        stored = entity
    }
}

private class FakeFocusSessionDao : FocusSessionDao {
    override suspend fun insert(session: FocusSessionEntity) = Unit
    override fun observeAll(): Flow<List<FocusSessionEntity>> = flowOf(emptyList())
}

private class FakeWeeklyHistoryRepository : WeeklyHistoryRepository {
    val episodes = mutableListOf<CrisisEpisodeRecord>()
    val savedSummaries = mutableListOf<WeeklySummary>()
    val prunedBefore = mutableListOf<Long>()

    /** Ordered log of the mutating calls, so tests can assert the prune happens after the save. */
    val callLog = mutableListOf<String>()

    override suspend fun recordCrisisEpisode(record: CrisisEpisodeRecord) {
        episodes += record
    }

    override suspend fun getCrisisEpisodesBetween(
        fromEpochMillis: Long,
        untilEpochMillis: Long
    ): List<CrisisEpisodeRecord> = episodes.filter {
        it.startedAtEpochMillis >= fromEpochMillis && it.startedAtEpochMillis < untilEpochMillis
    }

    override suspend fun deleteCrisisEpisodesBefore(beforeEpochMillis: Long) {
        callLog += "prune"
        prunedBefore += beforeEpochMillis
        episodes.removeAll { it.startedAtEpochMillis < beforeEpochMillis }
    }

    override suspend fun saveWeeklySummary(summary: WeeklySummary) {
        callLog += "save"
        savedSummaries += summary
    }

    override val weeklySummaries: Flow<List<WeeklySummary>> = flowOf(emptyList())
}

class AgencyRepositoryImplTest {

    private val agencyStateDao = FakeAgencyStateDao()
    private val weeklyHistory = FakeWeeklyHistoryRepository()
    private val repository = AgencyRepositoryImpl(
        agencyStateDao = agencyStateDao,
        focusSessionDao = FakeFocusSessionDao(),
        weeklyHistoryRepository = weeklyHistory
    )

    @Test
    fun `reporting the first state seeds the row`() = runBlocking {
        repository.reportAgencyState(AgencyState.Stable)

        assertEquals(1, agencyStateDao.upsertCount)
        assertEquals("STABLE", agencyStateDao.stored?.agencyState)
    }

    @Test
    fun `repeating the same state does not write again`() = runBlocking {
        repository.reportAgencyState(AgencyState.Stable)
        repeat(50) { repository.reportAgencyState(AgencyState.Stable) }

        assertEquals(1, agencyStateDao.upsertCount)
    }

    @Test
    fun `a genuine transition still writes`() = runBlocking {
        repository.reportAgencyState(AgencyState.Stable)
        repeat(20) { repository.reportAgencyState(AgencyState.Stable) }
        repository.reportAgencyState(AgencyState.Warning)
        repeat(20) { repository.reportAgencyState(AgencyState.Warning) }
        repository.reportAgencyState(AgencyState.Crisis)

        assertEquals(3, agencyStateDao.upsertCount)
        assertEquals("CRISIS", agencyStateDao.stored?.agencyState)
    }

    @Test
    fun `returning to a previously reported state writes again`() = runBlocking {
        repository.reportAgencyState(AgencyState.Crisis)
        repository.reportAgencyState(AgencyState.Stable)
        repository.reportAgencyState(AgencyState.Crisis)

        assertEquals(3, agencyStateDao.upsertCount)
    }

    @Test
    fun `skipping the write preserves the rest of the row`() = runBlocking {
        agencyStateDao.stored = AgencyStateEntity(
            agencyIndexValue = 73,
            agencyState = "STABLE",
            deltaAutonomy = 15,
            deltaSupport = 8,
            lastArchivedAgencyIndexValue = 61,
            lastWeeklyResetAtEpochMillis = 1_700_000_000_000L
        )

        repository.reportAgencyState(AgencyState.Stable)
        repository.reportAgencyState(AgencyState.Warning)

        assertEquals(1, agencyStateDao.upsertCount)
        val stored = agencyStateDao.stored
        assertEquals("WARNING", stored?.agencyState)
        assertEquals(73, stored?.agencyIndexValue)
        assertEquals(15, stored?.deltaAutonomy)
        assertEquals(8, stored?.deltaSupport)
        assertEquals(61, stored?.lastArchivedAgencyIndexValue)
        assertEquals(1_700_000_000_000L, stored?.lastWeeklyResetAtEpochMillis)
    }

    // --- Weekly-boundary pruning of crisis_episodes ---

    private fun dueForReset() {
        agencyStateDao.stored = AgencyStateEntity(
            agencyIndexValue = 60,
            agencyState = "STABLE",
            lastWeeklyResetAtEpochMillis = null
        )
    }

    @Test
    fun `the weekly reset prunes episodes belonging to the week it just summarized`() = runBlocking {
        val boundary = WeeklyResetCalculator.mostRecentResetBoundaryMillis(System.currentTimeMillis())
        val duringClosedWeek = boundary - 2 * 24 * 60 * 60 * 1000L
        val duringNewWeek = boundary + 60_000L
        dueForReset()
        weeklyHistory.episodes += CrisisEpisodeRecord(duringClosedWeek, "TEXT", true)
        weeklyHistory.episodes += CrisisEpisodeRecord(duringNewWeek, null, false)

        repository.currentAgencyIndex.first()

        assertEquals(listOf(boundary), weeklyHistory.prunedBefore)
        assertEquals(listOf(duringNewWeek), weeklyHistory.episodes.map { it.startedAtEpochMillis })
    }

    /**
     * Pins the ordering deliberately chosen at the call site: pruning before the summary is durable
     * would risk destroying the only copy of the data the summary is built from.
     */
    @Test
    fun `the prune runs only after the summary has been saved`() = runBlocking {
        dueForReset()
        weeklyHistory.episodes += CrisisEpisodeRecord(
            WeeklyResetCalculator.mostRecentResetBoundaryMillis(System.currentTimeMillis()) - 1000L,
            "HAPTIC",
            true
        )

        repository.currentAgencyIndex.first()

        assertEquals(listOf("save", "prune"), weeklyHistory.callLog)
    }

    /** The summary must be derived from the episodes, not emptied by the prune that follows it. */
    @Test
    fun `the summary still reflects the episodes that were then pruned`() = runBlocking {
        val boundary = WeeklyResetCalculator.mostRecentResetBoundaryMillis(System.currentTimeMillis())
        dueForReset()
        weeklyHistory.episodes += CrisisEpisodeRecord(boundary - 1000L, "HAPTIC", true)

        repository.currentAgencyIndex.first()

        assertEquals("HAPTIC", weeklyHistory.savedSummaries.single().mostEffectiveInterventionCategory)
        assertTrue(weeklyHistory.episodes.isEmpty())
    }

    /**
     * A reset that runs days late — the app simply wasn't opened on Sunday — must still summarize
     * only the week that closed. The running week's episodes deliberately outnumber the closed
     * week's here, so an unbounded query would tally BLUR as the most effective category.
     */
    @Test
    fun `a late reset leaves the running week out of the closed week's summary`() = runBlocking {
        val boundary = WeeklyResetCalculator.mostRecentResetBoundaryMillis(System.currentTimeMillis())
        dueForReset()
        weeklyHistory.episodes += CrisisEpisodeRecord(boundary - 1000L, "HAPTIC", true)
        weeklyHistory.episodes += CrisisEpisodeRecord(boundary + 1000L, "BLUR", true)
        weeklyHistory.episodes += CrisisEpisodeRecord(boundary + 2000L, "BLUR", true)

        repository.currentAgencyIndex.first()

        assertEquals("HAPTIC", weeklyHistory.savedSummaries.single().mostEffectiveInterventionCategory)
    }

    /**
     * Both the summary range and the prune are half-open at the boundary, so an episode landing
     * exactly on it belongs to the week starting there: it is neither summarized nor deleted, and
     * is therefore counted exactly once, next week. This is the edge where the two predicates have
     * to agree — disagreeing would either double-count it or destroy it unsummarized.
     */
    @Test
    fun `an episode exactly at the boundary is carried into the new week untouched`() = runBlocking {
        val boundary = WeeklyResetCalculator.mostRecentResetBoundaryMillis(System.currentTimeMillis())
        dueForReset()
        weeklyHistory.episodes += CrisisEpisodeRecord(boundary, "BLUR", true)

        repository.currentAgencyIndex.first()

        assertNull(weeklyHistory.savedSummaries.single().mostEffectiveInterventionCategory)
        assertEquals(listOf(boundary), weeklyHistory.episodes.map { it.startedAtEpochMillis })
    }

    @Test
    fun `an already-applied reset prunes nothing`() = runBlocking {
        val boundary = WeeklyResetCalculator.mostRecentResetBoundaryMillis(System.currentTimeMillis())
        agencyStateDao.stored = AgencyStateEntity(
            agencyIndexValue = 60,
            agencyState = "STABLE",
            lastWeeklyResetAtEpochMillis = boundary
        )
        weeklyHistory.episodes += CrisisEpisodeRecord(boundary - 1000L, null, false)

        repository.currentAgencyIndex.first()

        assertTrue(weeklyHistory.prunedBefore.isEmpty())
        assertEquals(1, weeklyHistory.episodes.size)
    }

    /**
     * The reads on this path used to go through `observe().firstOrNull()`, which subscribes to the
     * observable query and tears the subscription down again on every call. Against that version
     * this would have counted 51.
     */
    @Test
    fun `the per-scroll path never subscribes to the observable query`() = runBlocking {
        repeat(50) { repository.reportAgencyState(AgencyState.Stable) }
        repository.reportAgencyState(AgencyState.Crisis)

        assertEquals(0, agencyStateDao.subscriptionCount)
    }
}
