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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
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

    override val currentAgencyIndex: Flow<AgencyIndex> = agencyStateDao.observe()
        .filterNotNull()
        .map { applyWeeklyResetIfDue(AgencyIndex.of(it.agencyIndexValue)) }

    override val currentAgencyState: Flow<AgencyState> = agencyStateDao.observe()
        .filterNotNull()
        .map { it.agencyState.toAgencyState() }

    override suspend fun recordFocusSession(session: FocusSession) {
        focusSessionDao.insert(session.toEntity())
    }

    override suspend fun updateAgencyIndex(index: AgencyIndex) {
        val current = agencyStateDao.observe().firstOrNull()
        agencyStateDao.upsert(current.withDefaults().copy(agencyIndexValue = index.value))
    }

    override suspend fun reportAgencyState(state: AgencyState) {
        val current = agencyStateDao.observe().firstOrNull()
        agencyStateDao.upsert(current.withDefaults().copy(agencyState = state.toStorageName()))
    }

    override suspend fun adjustAgencyDeltas(deltaAutonomy: Int, deltaSupport: Int): AgencyIndex {
        val current = agencyStateDao.observe().firstOrNull()
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
        agencyStateDao.observe().firstOrNull()?.lastArchivedAgencyIndexValue

    override suspend fun archiveAgencyIndex(agencyIndexValue: Int) {
        val current = agencyStateDao.observe().firstOrNull()
        agencyStateDao.upsert(current.withDefaults().copy(lastArchivedAgencyIndexValue = agencyIndexValue))
    }

    override suspend fun getLastWeeklyResetAtEpochMillis(): Long? =
        agencyStateDao.observe().firstOrNull()?.lastWeeklyResetAtEpochMillis

    override suspend fun resetAgencyIndexForNewWeek(atEpochMillis: Long) {
        val current = agencyStateDao.observe().firstOrNull()
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

    private suspend fun applyWeeklyResetIfDue(index: AgencyIndex): AgencyIndex {
        val boundaryMillis = WeeklyResetCalculator.mostRecentResetBoundaryMillis(System.currentTimeMillis())
        val lastReset = getLastWeeklyResetAtEpochMillis()
        if (lastReset != null && lastReset >= boundaryMillis) return index

        val weekStartMillis = boundaryMillis - WeeklyResetCalculator.WEEK_DURATION_MILLIS
        val episodes = weeklyHistoryRepository.getCrisisEpisodesSince(weekStartMillis)
        val previousArchived = getLastArchivedAgencyIndex()
        val delta = index.value - (previousArchived ?: AgencyIndex.BASELINE)

        weeklyHistoryRepository.saveWeeklySummary(
            WeeklyResetCalculator.buildWeeklySummary(boundaryMillis, delta, episodes)
        )
        archiveAgencyIndex(index.value)
        resetAgencyIndexForNewWeek(boundaryMillis)
        return AgencyIndex.baseline()
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
