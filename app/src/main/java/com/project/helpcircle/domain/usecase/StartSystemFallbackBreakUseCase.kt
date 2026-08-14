package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.CrisisEpisodeTracker

/**
 * Records that the user committed to the System Fallback prompt's timed break, without awarding
 * any score yet. [DetectLossOfAgencyUseCase] verifies real elapsed time on the next scroll signal
 * before crediting it — tapping the button alone is deliberately not enough, closing the
 * instant-tap scoring exploit the naive version had.
 */
class StartSystemFallbackBreakUseCase(
    private val crisisEpisodeTracker: CrisisEpisodeTracker
) {
    operator fun invoke(atEpochMillis: Long) {
        crisisEpisodeTracker.onBreakStarted(atEpochMillis)
    }
}
