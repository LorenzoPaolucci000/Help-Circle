package com.project.helpcircle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room row for one generated weekly summary, keyed by the week's start timestamp. */
@Entity(tableName = "weekly_summaries")
data class WeeklySummaryEntity(
    @PrimaryKey val weekStartEpochMillis: Long,
    val agencyIndexDelta: Int,
    val mostEffectiveInterventionCategory: String?,
    val peakCrisisHour: Int?
)
