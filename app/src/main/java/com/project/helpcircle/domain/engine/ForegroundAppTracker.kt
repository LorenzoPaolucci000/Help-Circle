package com.project.helpcircle.domain.engine

/**
 * Tracks which app is currently in the foreground and when a blacklisted (monitored) app was last
 * confirmed there, as reported by the accessibility service's TYPE_WINDOW_STATE_CHANGED events.
 * In-memory only, like [CrisisEpisodeTracker]'s own episode state — this doesn't need to survive
 * process death for this MVP.
 */
class ForegroundAppTracker {
    var currentPackageName: String? = null
        private set

    /** Wall-clock time a monitored app was last confirmed in the foreground, or null if none has been observed yet. */
    var lastBlacklistedForegroundAtMillis: Long? = null
        private set

    fun onForegroundPackageChanged(packageName: String, atEpochMillis: Long, isBlacklisted: Boolean) {
        currentPackageName = packageName
        if (isBlacklisted) {
            lastBlacklistedForegroundAtMillis = atEpochMillis
        }
    }
}
