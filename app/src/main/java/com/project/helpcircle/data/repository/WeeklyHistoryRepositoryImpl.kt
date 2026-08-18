package com.project.helpcircle.data.repository

import com.project.helpcircle.data.local.dao.WeeklyHistoryDao
import com.project.helpcircle.data.local.entity.CrisisEpisodeEntity
import com.project.helpcircle.data.local.entity.WeeklySummaryEntity
import com.project.helpcircle.domain.model.CrisisEpisodeRecord
import com.project.helpcircle.domain.model.WeeklySummary
import com.project.helpcircle.domain.repository.WeeklyHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed [WeeklyHistoryRepository]: persists crisis-episode history and weekly summaries locally. */
class WeeklyHistoryRepositoryImpl @Inject constructor(
    private val weeklyHistoryDao: WeeklyHistoryDao
) : WeeklyHistoryRepository {

    override suspend fun recordCrisisEpisode(record: CrisisEpisodeRecord) {
        weeklyHistoryDao.insertCrisisEpisode(
            CrisisEpisodeEntity(
                startedAtEpochMillis = record.startedAtEpochMillis,
                nudgeCategory = record.nudgeCategory,
                wasEffectiveIntervention = record.wasEffectiveIntervention
            )
        )
    }

    override suspend fun getCrisisEpisodesBetween(
        fromEpochMillis: Long,
        untilEpochMillis: Long
    ): List<CrisisEpisodeRecord> =
        weeklyHistoryDao.getCrisisEpisodesBetween(fromEpochMillis, untilEpochMillis).map {
            CrisisEpisodeRecord(it.startedAtEpochMillis, it.nudgeCategory, it.wasEffectiveIntervention)
        }

    override suspend fun deleteCrisisEpisodesBefore(beforeEpochMillis: Long) {
        weeklyHistoryDao.deleteCrisisEpisodesBefore(beforeEpochMillis)
    }

    override suspend fun saveWeeklySummary(summary: WeeklySummary) {
        weeklyHistoryDao.insertWeeklySummary(
            WeeklySummaryEntity(
                weekStartEpochMillis = summary.weekStartEpochMillis,
                agencyIndexDelta = summary.agencyIndexDelta,
                mostEffectiveInterventionCategory = summary.mostEffectiveInterventionCategory,
                peakCrisisHour = summary.peakCrisisHour
            )
        )
    }

    override val weeklySummaries: Flow<List<WeeklySummary>> = weeklyHistoryDao.observeWeeklySummaries().map { entities ->
        entities.map { WeeklySummary(it.weekStartEpochMillis, it.agencyIndexDelta, it.mostEffectiveInterventionCategory, it.peakCrisisHour) }
    }
}
