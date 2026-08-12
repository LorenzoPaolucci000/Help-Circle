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

    @Query("SELECT * FROM crisis_episodes WHERE startedAtEpochMillis >= :sinceEpochMillis ORDER BY startedAtEpochMillis")
    suspend fun getCrisisEpisodesSince(sinceEpochMillis: Long): List<CrisisEpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklySummary(summary: WeeklySummaryEntity)

    @Query("SELECT * FROM weekly_summaries ORDER BY weekStartEpochMillis DESC")
    fun observeWeeklySummaries(): Flow<List<WeeklySummaryEntity>>
}
