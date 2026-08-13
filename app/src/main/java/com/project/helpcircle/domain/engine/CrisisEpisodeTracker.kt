package com.project.helpcircle.domain.engine

import com.project.helpcircle.domain.model.AgencyState

/** A completed crisis episode, kept only long enough for the caller to persist it into local weekly-summary history. */
data class ClosedCrisisEpisode(
    val startedAtEpochMillis: Long,
    val nudgeCategory: String?,
    val wasEffectiveIntervention: Boolean
)

/** A Delta_Autonomy/Delta_Support adjustment a crisis-episode transition earns, per [DetectionConfig]. */
data class AgencyDelta(
    val deltaAutonomy: Int = 0,
    val deltaSupport: Int = 0,
    val closedEpisode: ClosedCrisisEpisode? = null
) {
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
    private var nudgeCategory: String? = null
    private var ignoredPenaltyApplied = false
    private var activePresenceApplied = false

    /** Marks that a nudge of [category] was delivered during the current crisis episode; only the first counts. */
    fun onNudgeReceived(atEpochMillis: Long, category: String) {
        if (crisisStartedAtMillis != null && nudgeReceivedAtMillis == null) {
            nudgeReceivedAtMillis = atEpochMillis
            nudgeCategory = category
        }
    }

    /** When the current crisis episode started, or null if none is open. Read-only; callers can't affect scoring through it. */
    fun currentCrisisStartedAtMillis(): Long? = crisisStartedAtMillis

    /** Whether a peer's nudge has already been delivered during the current crisis episode. */
    fun hasReceivedNudgeThisEpisode(): Boolean = nudgeReceivedAtMillis != null

    /** Feeds a new [AgencyState] reading and returns any [AgencyDelta] this transition/tick earns. */
    fun onAgencyStateUpdated(state: AgencyState, atEpochMillis: Long): AgencyDelta {
        val wasInCrisis = crisisStartedAtMillis != null

        if (state == AgencyState.Crisis && !wasInCrisis) {
            crisisStartedAtMillis = atEpochMillis
            nudgeReceivedAtMillis = null
            nudgeCategory = null
            ignoredPenaltyApplied = false
            activePresenceApplied = false
            return AgencyDelta.NONE
        }

        if (!wasInCrisis) return AgencyDelta.NONE

        val startedAt = crisisStartedAtMillis ?: return AgencyDelta.NONE
        val nudgeAt = nudgeReceivedAtMillis

        if (state == AgencyState.Crisis) {
            var deltaAutonomy = 0
            var deltaSupport = 0
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
            return if (deltaAutonomy == 0 && deltaSupport == 0) AgencyDelta.NONE else AgencyDelta(deltaAutonomy, deltaSupport)
        }

        // The episode is ending: score it, then hand back a ClosedCrisisEpisode regardless of
        // whether it earned a delta, so the caller can still record it for the weekly summary's
        // peak-crisis-hours/intervention-category stats.
        var deltaAutonomy = 0
        var deltaSupport = 0
        if (atEpochMillis - startedAt <= DetectionConfig.SPONTANEOUS_RECOVERY_WINDOW_MS) {
            deltaAutonomy += DetectionConfig.SPONTANEOUS_RECOVERY_DELTA
        }
        val wasEffectiveIntervention = nudgeAt != null &&
            atEpochMillis - nudgeAt <= DetectionConfig.EFFECTIVE_INTERVENTION_WINDOW_MS
        if (wasEffectiveIntervention) {
            deltaSupport += DetectionConfig.EFFECTIVE_INTERVENTION_DELTA
        }
        val closedEpisode = ClosedCrisisEpisode(startedAt, nudgeCategory, wasEffectiveIntervention)

        crisisStartedAtMillis = null
        nudgeReceivedAtMillis = null
        nudgeCategory = null
        ignoredPenaltyApplied = false
        activePresenceApplied = false

        return AgencyDelta(deltaAutonomy, deltaSupport, closedEpisode)
    }
}
