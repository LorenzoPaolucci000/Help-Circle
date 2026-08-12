package com.project.helpcircle.domain.model

/** A completed crisis episode, kept locally only to compute the weekly summary — never synced anywhere. */
data class CrisisEpisodeRecord(
    val startedAtEpochMillis: Long,
    val nudgeCategory: String?,
    val wasEffectiveIntervention: Boolean
)
