package com.project.helpcircle.domain.repository

import com.project.helpcircle.domain.model.CrisisEpisodeRecord
import com.project.helpcircle.domain.model.WeeklySummary
import kotlinx.coroutines.flow.Flow

/**
 * Local-only history of crisis episodes and generated weekly summaries; nothing here is ever
 * synced to Firestore. The live IA_ind/weekly-reset bookkeeping itself lives on [AgencyRepository]
 * since it operates on the same singleton row that state is stored in.
 */
interface WeeklyHistoryRepository {
    suspend fun recordCrisisEpisode(record: CrisisEpisodeRecord)
    /**
     * Episodes started within the half-open range `[fromEpochMillis, untilEpochMillis)`. The upper
     * bound is what keeps a late-running weekly reset from folding the running week's episodes into
     * the summary of the week that already closed, and then counting them again a week later.
     */
    suspend fun getCrisisEpisodesBetween(fromEpochMillis: Long, untilEpochMillis: Long): List<CrisisEpisodeRecord>

    /**
     * Drops episodes started before [beforeEpochMillis], which the caller has already summarized.
     * Episodes exist only to feed the weekly summary, so retaining them past their week's boundary
     * serves nothing and leaves this the one table that would otherwise grow without limit.
     */
    suspend fun deleteCrisisEpisodesBefore(beforeEpochMillis: Long)

    suspend fun saveWeeklySummary(summary: WeeklySummary)
    val weeklySummaries: Flow<List<WeeklySummary>>
}
