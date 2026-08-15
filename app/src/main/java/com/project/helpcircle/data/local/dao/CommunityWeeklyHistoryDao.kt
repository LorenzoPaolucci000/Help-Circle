package com.project.helpcircle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.helpcircle.data.local.entity.CommunityWeeklySummaryEntity
import kotlinx.coroutines.flow.Flow

/** Room DAO for the local-only history of IA_comm snapshots this device has recorded per community. */
@Dao
interface CommunityWeeklyHistoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSummary(summary: CommunityWeeklySummaryEntity)

    @Query("SELECT * FROM community_weekly_summaries WHERE communityId = :communityId ORDER BY weekStartEpochMillis DESC")
    fun observeSummaries(communityId: String): Flow<List<CommunityWeeklySummaryEntity>>

    @Query("SELECT weekStartEpochMillis FROM community_weekly_summaries WHERE communityId = :communityId ORDER BY weekStartEpochMillis DESC LIMIT 1")
    suspend fun getLatestWeekStartEpochMillis(communityId: String): Long?
}
