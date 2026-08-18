package com.project.helpcircle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.helpcircle.data.local.entity.CrisisEpisodeEntity
import com.project.helpcircle.data.local.entity.WeeklySummaryEntity
import kotlinx.coroutines.flow.Flow

/** Room DAO for the local-only crisis-episode history and generated weekly summaries. */
@Dao
interface WeeklyHistoryDao {
    @Insert
    suspend fun insertCrisisEpisode(episode: CrisisEpisodeEntity)

    /**
     * Episodes belonging to the half-open week `[fromEpochMillis, untilEpochMillis)`. Bounded at
     * both ends: a weekly reset that runs late — the app simply isn't opened until midweek — would
     * otherwise sweep up episodes that started *after* the boundary being summarized, attributing
     * days of the running week to the week that already closed and then counting them a second
     * time when that running week is itself summarized.
     */
    @Query(
        "SELECT * FROM crisis_episodes " +
            "WHERE startedAtEpochMillis >= :fromEpochMillis AND startedAtEpochMillis < :untilEpochMillis " +
            "ORDER BY startedAtEpochMillis"
    )
    suspend fun getCrisisEpisodesBetween(fromEpochMillis: Long, untilEpochMillis: Long): List<CrisisEpisodeEntity>

    /**
     * Discards episodes that have already been folded into a weekly summary. Individual episodes
     * are only ever read to build that summary, so once the week they belong to has closed they
     * can never be read again — keeping them would grow this table for the lifetime of the install
     * and retain behavioural records past the point they serve any purpose.
     */
    @Query("DELETE FROM crisis_episodes WHERE startedAtEpochMillis < :beforeEpochMillis")
    suspend fun deleteCrisisEpisodesBefore(beforeEpochMillis: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklySummary(summary: WeeklySummaryEntity)

    @Query("SELECT * FROM weekly_summaries ORDER BY weekStartEpochMillis DESC")
    fun observeWeeklySummaries(): Flow<List<WeeklySummaryEntity>>
}
