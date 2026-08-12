package com.project.helpcircle.domain.repository

import com.project.helpcircle.domain.model.CrisisEpisodeRecord
import com.project.helpcircle.domain.model.WeeklySummary
import kotlinx.coroutines.flow.Flow

/** Local-only history backing the weekly reset/summary; nothing here is ever synced to Firestore. */
interface WeeklyHistoryRepository {
    suspend fun recordCrisisEpisode(record: CrisisEpisodeRecord)
    suspend fun getCrisisEpisodesSince(sinceEpochMillis: Long): List<CrisisEpisodeRecord>

    /** The last IA_ind value archived at a weekly boundary, or null if none has been archived yet. */
    suspend fun getLastArchivedAgencyIndex(): Int?
    suspend fun archiveAgencyIndex(atEpochMillis: Long, agencyIndexValue: Int)

    suspend fun getLastWeeklyResetAtEpochMillis(): Long?
    suspend fun setLastWeeklyResetAtEpochMillis(epochMillis: Long)

    suspend fun saveWeeklySummary(summary: WeeklySummary)
    val weeklySummaries: Flow<List<WeeklySummary>>
}
