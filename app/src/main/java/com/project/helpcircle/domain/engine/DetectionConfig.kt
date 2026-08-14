package com.project.helpcircle.domain.engine

/**
 * Tunable point values and time windows for the IA_ind delta triggers, centralized here per the
 * thesis's formula so none of the trigger call sites hardcode their own magic numbers:
 * Delta_Autonomy = [SPONTANEOUS_RECOVERY_DELTA] * S_recovery - [SPONTANEOUS_RECOVERY_DELTA] * S_ignored
 * Delta_Support = [EFFECTIVE_INTERVENTION_DELTA] * I_effective + [ACTIVE_PRESENCE_DELTA] * I_attempted
 */
object DetectionConfig {
    // Delta_Autonomy modifiers

    /** Awarded when the user terminates scrolling within [SPONTANEOUS_RECOVERY_WINDOW_MS] of crisis detection. */
    const val SPONTANEOUS_RECOVERY_DELTA = 5

    /** Applied when the user keeps scrolling past [IGNORED_CRISIS_THRESHOLD_MS] despite alerts. */
    const val IGNORED_CRISIS_DELTA = -5

    /** No peer responding is never penalized — peer unavailability isn't the user's fault. */
    const val UNASSISTED_TIMEOUT_DELTA = 0

    /**
     * Awarded, on top of (not instead of) whatever [SPONTANEOUS_RECOVERY_DELTA] check applies, when
     * the System Fallback prompt's timed break is genuinely completed — i.e. [SYSTEM_FALLBACK_BREAK_DURATION_MS]
     * of real elapsed time is confirmed before the recovery is scored, not merely tapped. Kept
     * separate from the base thesis formula's spontaneous-recovery term, which this deliberately
     * doesn't replace: a break is deliberately slower than the 60s spontaneous-recovery window, so
     * without this it would otherwise net zero Delta_Autonomy despite being the *intended*, honest
     * use of the tool.
     */
    const val ASSISTED_BREAK_COMPLETION_DELTA = 3

    // Delta_Support modifiers

    /** Awarded when a supporter's nudge is followed by the user exiting the app within [EFFECTIVE_INTERVENTION_WINDOW_MS]. */
    const val EFFECTIVE_INTERVENTION_DELTA = 10

    /** Awarded when a supporter sends a nudge but the user doesn't exit the app. */
    const val ACTIVE_PRESENCE_DELTA = 2

    // Thresholds

    const val SPONTANEOUS_RECOVERY_WINDOW_MS = 60 * 1000L
    const val IGNORED_CRISIS_THRESHOLD_MS = 3 * 60 * 1000L
    const val EFFECTIVE_INTERVENTION_WINDOW_MS = 90 * 1000L

    /** Real elapsed time required before a System Fallback break counts as genuinely taken. */
    const val SYSTEM_FALLBACK_BREAK_DURATION_MS = 2 * 60 * 1000L
}
