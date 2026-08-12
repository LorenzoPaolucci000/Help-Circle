package com.project.helpcircle.domain.engine

import com.project.helpcircle.domain.model.AgencyState

/** A Delta_Autonomy/Delta_Support adjustment a crisis-episode transition earns, per [DetectionConfig]. */
data class AgencyDelta(val deltaAutonomy: Int = 0, val deltaSupport: Int = 0) {
    val isEmpty: Boolean get() = deltaAutonomy == 0 && deltaSupport == 0

    companion object {
        val NONE = AgencyDelta()
    }
}

/**
 * Tracks a single crisis episode's timing against [DetectionConfig]'s windows/thresholds and
 * produces the [AgencyDelta] each [AgencyState] reading earns. In-memory only, like
 * [AgencyDetectionEngine]'s own sliding window — episode timing doesn't need to survive process
 * death for this MVP.
 */
class CrisisEpisodeTracker {
    private var crisisStartedAtMillis: Long? = null
    private var nudgeReceivedAtMillis: Long? = null
    private var ignoredPenaltyApplied = false
    private var activePresenceApplied = false

    /** Marks that a nudge was delivered during the current crisis episode; only the first counts. */
    fun onNudgeReceived(atEpochMillis: Long) {
        if (crisisStartedAtMillis != null && nudgeReceivedAtMillis == null) {
            nudgeReceivedAtMillis = atEpochMillis
        }
    }

    /** Feeds a new [AgencyState] reading and returns any [AgencyDelta] this transition/tick earns. */
    fun onAgencyStateUpdated(state: AgencyState, atEpochMillis: Long): AgencyDelta {
        val wasInCrisis = crisisStartedAtMillis != null

        if (state == AgencyState.Crisis && !wasInCrisis) {
            crisisStartedAtMillis = atEpochMillis
            nudgeReceivedAtMillis = null
            ignoredPenaltyApplied = false
            activePresenceApplied = false
            return AgencyDelta.NONE
        }

        if (!wasInCrisis) return AgencyDelta.NONE

        val startedAt = crisisStartedAtMillis ?: return AgencyDelta.NONE
        val nudgeAt = nudgeReceivedAtMillis
        var deltaAutonomy = 0
        var deltaSupport = 0

        if (state == AgencyState.Crisis) {
            // Scrolling continues past the ignored-crisis threshold despite the system's fallback
            // alerts; this fires purely on elapsed time, never on peer/nudge availability, so a
            // crisis with no peer around is never penalized (UNASSISTED_TIMEOUT_DELTA = 0).
            if (!ignoredPenaltyApplied && atEpochMillis - startedAt > DetectionConfig.IGNORED_CRISIS_THRESHOLD_MS) {
                ignoredPenaltyApplied = true
                deltaAutonomy += DetectionConfig.IGNORED_CRISIS_DELTA
            }
            if (nudgeAt != null && !activePresenceApplied &&
                atEpochMillis - nudgeAt > DetectionConfig.EFFECTIVE_INTERVENTION_WINDOW_MS
            ) {
                activePresenceApplied = true
                deltaSupport += DetectionConfig.ACTIVE_PRESENCE_DELTA
            }
        } else {
            if (atEpochMillis - startedAt <= DetectionConfig.SPONTANEOUS_RECOVERY_WINDOW_MS) {
                deltaAutonomy += DetectionConfig.SPONTANEOUS_RECOVERY_DELTA
            }
            if (nudgeAt != null && atEpochMillis - nudgeAt <= DetectionConfig.EFFECTIVE_INTERVENTION_WINDOW_MS) {
                deltaSupport += DetectionConfig.EFFECTIVE_INTERVENTION_DELTA
            }
            crisisStartedAtMillis = null
            nudgeReceivedAtMillis = null
            ignoredPenaltyApplied = false
            activePresenceApplied = false
        }

        return if (deltaAutonomy == 0 && deltaSupport == 0) AgencyDelta.NONE else AgencyDelta(deltaAutonomy, deltaSupport)
    }
}
