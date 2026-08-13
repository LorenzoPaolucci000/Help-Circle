package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.AgencyDetectionEngine
import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.engine.ScrollSignal
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.CrisisEpisodeRecord
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.WeeklyHistoryRepository

/** The doomscroll-risk reading a signal produced, plus whether the system's autonomous fallback prompt is now due. */
data class LossOfAgencyResult(
    val state: AgencyState,
    val offerSystemFallback: Boolean
)

/**
 * Feeds a scroll signal into [AgencyDetectionEngine], reports the resulting [AgencyState], and
 * lets [CrisisEpisodeTracker] award/penalize the IA_ind deltas that transition earns (spontaneous
 * recovery, an ignored crisis, or a crisis-episode's nudge outcome) via [CalculateAgencyIndexUseCase].
 * Whenever the tracker reports a closed episode, it's archived to [WeeklyHistoryRepository] for the
 * weekly summary's peak-crisis-hours/intervention-category stats. Also asks
 * [EvaluateSystemFallbackUseCase] whether the community is offline or unresponsive enough that the
 * system should autonomously offer its own break prompt, per the crisis-fallback spec.
 */
class DetectLossOfAgencyUseCase(
    private val agencyDetectionEngine: AgencyDetectionEngine,
    private val agencyRepository: AgencyRepository,
    private val crisisEpisodeTracker: CrisisEpisodeTracker,
    private val calculateAgencyIndexUseCase: CalculateAgencyIndexUseCase,
    private val weeklyHistoryRepository: WeeklyHistoryRepository,
    private val evaluateSystemFallbackUseCase: EvaluateSystemFallbackUseCase
) {
    suspend operator fun invoke(signal: ScrollSignal): LossOfAgencyResult {
        val state = agencyDetectionEngine.record(signal)
        agencyRepository.reportAgencyState(state)
        val delta = crisisEpisodeTracker.onAgencyStateUpdated(state, signal.timestampMillis)
        if (!delta.isEmpty) {
            calculateAgencyIndexUseCase(delta.deltaAutonomy, delta.deltaSupport)
        }
        delta.closedEpisode?.let { episode ->
            weeklyHistoryRepository.recordCrisisEpisode(
                CrisisEpisodeRecord(episode.startedAtEpochMillis, episode.nudgeCategory, episode.wasEffectiveIntervention)
            )
        }
        val offerSystemFallback = evaluateSystemFallbackUseCase(state, signal.timestampMillis)
        return LossOfAgencyResult(state, offerSystemFallback)
    }
}
