package com.project.helpcircle.data.local.entity

import androidx.room.Entity

/** Room row for one community's IA_comm snapshot at a weekly boundary, keyed by which community and which week. */
@Entity(tableName = "community_weekly_summaries", primaryKeys = ["communityId", "weekStartEpochMillis"])
data class CommunityWeeklySummaryEntity(
    val communityId: String,
    val weekStartEpochMillis: Long,
    val collectiveIndexValue: Int
)
