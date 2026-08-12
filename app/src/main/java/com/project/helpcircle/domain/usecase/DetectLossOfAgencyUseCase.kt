package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.AgencyDetectionEngine
import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.engine.ScrollSignal
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.repository.AgencyRepository

/**
 * Feeds a scroll signal into [AgencyDetectionEngine], reports the resulting [AgencyState], and
 * lets [CrisisEpisodeTracker] award/penalize the IA_ind deltas that transition earns (spontaneous
 * recovery, an ignored crisis, or a crisis-episode's nudge outcome) via [CalculateAgencyIndexUseCase].
 */
class DetectLossOfAgencyUseCase(
    private val agencyDetectionEngine: AgencyDetectionEngine,
    private val agencyRepository: AgencyRepository,
    private val crisisEpisodeTracker: CrisisEpisodeTracker,
    private val calculateAgencyIndexUseCase: CalculateAgencyIndexUseCase
) {
    suspend operator fun invoke(signal: ScrollSignal): AgencyState {
        val state = agencyDetectionEngine.record(signal)
        agencyRepository.reportAgencyState(state)
        val delta = crisisEpisodeTracker.onAgencyStateUpdated(state, signal.timestampMillis)
        if (!delta.isEmpty) {
            calculateAgencyIndexUseCase(delta.deltaAutonomy, delta.deltaSupport)
        }
        return state
    }
}
