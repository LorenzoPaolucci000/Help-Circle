package com.project.helpcircle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for this device's own self-reported satisfaction rating for one week, keyed by which
 * week it describes so re-rating the same week replaces it rather than accumulating duplicates.
 *
 * [satisfaction] is stored as the enum's name rather than its ordinal: the database survives app
 * updates, and a name keeps the stored value meaningful even if the enum's declaration order ever
 * changes.
 */
@Entity(tableName = "weekly_satisfaction")
data class WeeklySatisfactionEntity(
    @PrimaryKey val weekStartEpochMillis: Long,
    val satisfaction: String
)
