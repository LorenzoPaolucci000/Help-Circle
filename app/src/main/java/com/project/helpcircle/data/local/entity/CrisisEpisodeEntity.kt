package com.project.helpcircle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room row for one completed crisis episode, kept locally only to compute the weekly summary. */
@Entity(tableName = "crisis_episodes")
data class CrisisEpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMillis: Long,
    val nudgeCategory: String?,
    val wasEffectiveIntervention: Boolean
)
