package com.project.helpcircle.domain.engine

import com.project.helpcircle.domain.model.AgencyState

/**
 * Decides, once per crisis episode, whether the system should autonomously offer its fallback
 * break prompt: immediately once the community is known to be offline (no peers to notify), or
 * once [DetectionConfig.IGNORED_CRISIS_THRESHOLD_MS] has passed with no peer's nudge received.
 * Reads [CrisisEpisodeTracker]'s own episode timing rather than duplicating it, so this stays a
 * thin decision layer with its own one-shot-per-episode bookkeeping naturally keyed to the same
 * episode start time.
 */
class SystemFallbackEvaluator(
    private val crisisEpisodeTracker: CrisisEpisodeTracker
) {
    private var offeredForEpisodeStartedAt: Long? = null

    /** Returns true exactly once per crisis episode, the first tick the offer condition is met. */
    fun evaluate(state: AgencyState, atEpochMillis: Long, communityOffline: Boolean): Boolean {
        if (state != AgencyState.Crisis) return false
        val startedAt = crisisEpisodeTracker.currentCrisisStartedAtMillis() ?: return false
        if (offeredForEpisodeStartedAt == startedAt) return false

        val timedOut = !crisisEpisodeTracker.hasReceivedNudgeThisEpisode() &&
            atEpochMillis - startedAt > DetectionConfig.IGNORED_CRISIS_THRESHOLD_MS
        if (!communityOffline && !timedOut) return false

        offeredForEpisodeStartedAt = startedAt
        return true
    }
}
