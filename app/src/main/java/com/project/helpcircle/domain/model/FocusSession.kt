package com.project.helpcircle.domain.model

/** A completed monitoring session and the worst [AgencyState] it reached. */
data class FocusSession(
    val id: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val peakAgencyState: AgencyState
)
