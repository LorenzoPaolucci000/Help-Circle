package com.project.helpcircle.data.repository

import com.project.helpcircle.data.local.dao.AgencyStateDao
import com.project.helpcircle.data.local.dao.FocusSessionDao
import com.project.helpcircle.data.local.entity.AgencyStateEntity
import com.project.helpcircle.data.local.entity.FocusSessionEntity
import com.project.helpcircle.domain.engine.WeeklyResetCalculator
import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.FocusSession
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.WeeklyHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed [AgencyRepository]: persists the current agency index/state and session history
 * locally. [currentAgencyIndex] also lazily catches up the weekly reset (archive → reset to
 * baseline → generate summary) against the current time on every read — same no-background-job
 * pattern as [ObserveChargeWalletUseCase][com.project.helpcircle.domain.usecase.ObserveChargeWalletUseCase] —
 * so every reader (including [CommunityRepositoryImpl]'s Firestore sync) transparently sees an
 * up-to-date index without needing to know about the reset at all.
 */
class AgencyRepositoryImpl @Inject constructor(
    private val agencyStateDao: AgencyStateDao,
    private val focusSessionDao: FocusSessionDao,
    private val weeklyHistoryRepository: WeeklyHistoryRepository
) : AgencyRepository {

    // The lazy catch-up runs once here, before ever subscribing to agencyStateDao.observe(),
    // rather than as a map() over that same observed table's own emissions: writing to a table
    // from inside a transform that's reacting to that table's own invalidations is a Room/
    // SQLCipher self-triggering pattern that was found to hang indefinitely on-device (the
    // upsert's invalidation re-enters the in-flight collection of the very flow doing the
    // upserting). Running the check as a one-shot suspend step ahead of emitAll(...) means the
    // write — and everything it invalidates — is fully settled before observation ever starts.
    override val currentAgencyIndex: Flow<AgencyIndex> = flow {
        ensureWeeklyResetApplied()
        emitAll(agencyStateDao.observe().filterNotNull().map { AgencyIndex.of(it.agencyIndexValue) })
    }

    override val currentAgencyState: Flow<AgencyState> = agencyStateDao.observe()
        .filterNotNull()
        .map { it.agencyState.toAgencyState() }

    override suspend fun recordFocusSession(session: FocusSession) {
        focusSessionDao.insert(session.toEntity())
    }

    override suspend fun updateAgencyIndex(index: AgencyIndex) {
        val current = agencyStateDao.get()
        agencyStateDao.upsert(current.withDefaults().copy(agencyIndexValue = index.value))
    }

    /**
     * Called for every scroll event the accessibility service forwards, so it deliberately writes
     * nothing when the state is unchanged — which is the overwhelmingly common case, since a run of
     * scrolls reports Stable (or Crisis) over and over before it ever transitions. Persisting an
     * identical row on each one would mean an encrypted write and a Room invalidation per scroll,
     * for no observable change, against the battery-drain constraint this service is held to.
     */
    override suspend fun reportAgencyState(state: AgencyState) {
        val storageName = state.toStorageName()
        val current = agencyStateDao.get()
        if (current?.agencyState == storageName) return
        agencyStateDao.upsert(current.withDefaults().copy(agencyState = storageName))
    }

    override suspend fun adjustAgencyDeltas(deltaAutonomy: Int, deltaSupport: Int): AgencyIndex {
        val current = agencyStateDao.get()
        val newDeltaAutonomy = (current?.deltaAutonomy ?: 0) + deltaAutonomy
        val newDeltaSupport = (current?.deltaSupport ?: 0) + deltaSupport
        val index = AgencyIndex.calculate(newDeltaAutonomy, newDeltaSupport)
        agencyStateDao.upsert(
            current.withDefaults().copy(
                agencyIndexValue = index.value,
                deltaAutonomy = newDeltaAutonomy,
                deltaSupport = newDeltaSupport
            )
        )
        return index
    }

    override suspend fun getLastArchivedAgencyIndex(): Int? =
        agencyStateDao.get()?.lastArchivedAgencyIndexValue

    override suspend fun archiveAgencyIndex(agencyIndexValue: Int) {
        val current = agencyStateDao.get()
        agencyStateDao.upsert(current.withDefaults().copy(lastArchivedAgencyIndexValue = agencyIndexValue))
    }

    override suspend fun getLastWeeklyResetAtEpochMillis(): Long? =
        agencyStateDao.get()?.lastWeeklyResetAtEpochMillis

    override suspend fun resetAgencyIndexForNewWeek(atEpochMillis: Long) {
        val current = agencyStateDao.get()
        agencyStateDao.upsert(
            current.withDefaults().copy(
                agencyIndexValue = AgencyIndex.BASELINE,
                deltaAutonomy = 0,
                deltaSupport = 0,
                lastWeeklyResetAtEpochMillis = atEpochMillis
            )
        )
    }

    /** Fills in a fresh row's defaults so every upsert site can just `.copy()` the fields it actually changes. */
    private fun AgencyStateEntity?.withDefaults(): AgencyStateEntity = this ?: AgencyStateEntity(
        agencyIndexValue = AgencyIndex.BASELINE,
        agencyState = AgencyState.Stable.toStorageName()
    )

    private suspend fun ensureWeeklyResetApplied() {
        val boundaryMillis = WeeklyResetCalculator.mostRecentResetBoundaryMillis(System.currentTimeMillis())
        val lastReset = getLastWeeklyResetAtEpochMillis()
        if (lastReset != null && lastReset >= boundaryMillis) return

        val currentIndexValue = agencyStateDao.get()?.agencyIndexValue ?: AgencyIndex.BASELINE
        val weekStartMillis = boundaryMillis - WeeklyResetCalculator.WEEK_DURATION_MILLIS
        // Same half-open range the prune below deletes, so the episodes folded into this summary and
        // the episodes discarded afterwards are by construction the same set.
        val episodes = weeklyHistoryRepository.getCrisisEpisodesBetween(weekStartMillis, boundaryMillis)
        val previousArchived = getLastArchivedAgencyIndex()
        val delta = currentIndexValue - (previousArchived ?: AgencyIndex.BASELINE)

        weeklyHistoryRepository.saveWeeklySummary(
            WeeklyResetCalculator.buildWeeklySummary(boundaryMillis, delta, episodes)
        )
        archiveAgencyIndex(currentIndexValue)
        resetAgencyIndexForNewWeek(boundaryMillis)

        // Deliberately last, after resetAgencyIndexForNewWeek() has committed the marker that makes
        // this whole method a no-op until the next boundary. Dying between the two would otherwise
        // leave the summary saved but the marker unwritten, so the next run would rebuild that same
        // week from episodes this line had already deleted and overwrite a good summary with an
        // empty one. In this order a crash merely leaves the old rows in place, and the following
        // week's prune — which deletes everything before a later boundary — clears them anyway.
        weeklyHistoryRepository.deleteCrisisEpisodesBefore(boundaryMillis)
    }
}

private fun AgencyState.toStorageName(): String = when (this) {
    AgencyState.Stable -> "STABLE"
    AgencyState.Warning -> "WARNING"
    AgencyState.Crisis -> "CRISIS"
}

private fun String.toAgencyState(): AgencyState = when (this) {
    "WARNING" -> AgencyState.Warning
    "CRISIS" -> AgencyState.Crisis
    else -> AgencyState.Stable
}

private fun FocusSession.toEntity(): FocusSessionEntity = FocusSessionEntity(
    id = id,
    startedAtEpochMillis = startedAtEpochMillis,
    endedAtEpochMillis = endedAtEpochMillis,
    peakAgencyState = peakAgencyState.toStorageName()
)
