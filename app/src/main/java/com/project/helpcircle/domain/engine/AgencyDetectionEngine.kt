package com.project.helpcircle.domain.engine

import com.project.helpcircle.domain.model.AgencyState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** A single scroll/tap event timestamp fed into [AgencyDetectionEngine]. */
data class ScrollSignal(val timestampMillis: Long)

/**
 * Local sliding-window doomscrolling detector. Counts scroll signals within [windowSize] of the
 * most recent signal; [scrollThreshold] signals in that window is treated as Loss of Agency.
 */
class AgencyDetectionEngine(
    private val windowSize: Duration = 60.seconds,
    private val scrollThreshold: Int = 40,
    private val warningRatio: Double = 0.6
) {
    private val recentSignals = ArrayDeque<Long>()

    fun record(signal: ScrollSignal): AgencyState {
        recentSignals.addLast(signal.timestampMillis)
        val windowStart = signal.timestampMillis - windowSize.inWholeMilliseconds
        while (recentSignals.isNotEmpty() && recentSignals.first() < windowStart) {
            recentSignals.removeFirst()
        }

        val countInWindow = recentSignals.size
        val warningThreshold = (scrollThreshold * warningRatio).toInt()
        return when {
            countInWindow >= scrollThreshold -> AgencyState.Crisis
            countInWindow >= warningThreshold -> AgencyState.Warning
            else -> AgencyState.Stable
        }
    }

    fun reset() {
        recentSignals.clear()
    }
}
