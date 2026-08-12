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
    suspend fun getCrisisEpisodesSince(sinceEpochMillis: Long): List<CrisisEpisodeRecord>

    suspend fun saveWeeklySummary(summary: WeeklySummary)
    val weeklySummaries: Flow<List<WeeklySummary>>
}
