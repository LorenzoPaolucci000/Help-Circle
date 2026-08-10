package com.project.helpcircle.domain.model

/** The user's current doomscrolling risk level, as classified by AgencyDetectionEngine. */
sealed interface AgencyState {
    data object Stable : AgencyState
    data object Warning : AgencyState
    data object Crisis : AgencyState
}
