package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.ForegroundAppTracker

private const val FOCUS_MODE_IDLE_THRESHOLD_MILLIS = 15 * 60 * 1000L

/**
 * True when no monitored (blacklisted) app has held the foreground for at least the last 15
 * minutes — the faster charge-replenishment rate applies while this holds. True by default when
 * [ForegroundAppTracker] has never observed a blacklisted app at all.
 */
class IsFocusModeActiveUseCase(
    private val foregroundAppTracker: ForegroundAppTracker
) {
    operator fun invoke(nowEpochMillis: Long): Boolean {
        val lastBlacklistedAt = foregroundAppTracker.lastBlacklistedForegroundAtMillis ?: return true
        return nowEpochMillis - lastBlacklistedAt >= FOCUS_MODE_IDLE_THRESHOLD_MILLIS
    }
}
