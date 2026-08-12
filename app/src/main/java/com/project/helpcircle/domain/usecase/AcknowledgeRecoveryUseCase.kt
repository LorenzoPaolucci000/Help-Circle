package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.repository.AgencyRepository

/**
 * Explicitly closes out the current crisis episode when the user taps "I'm back" on a nudge
 * notification. This complements [DetectLossOfAgencyUseCase]'s passive detection: once a user
 * leaves the monitored app, no further scroll/tap accessibility events arrive to re-evaluate the
 * sliding window against, so without this explicit signal a crisis episode could otherwise be
 * left open indefinitely.
 */
class AcknowledgeRecoveryUseCase(
    private val agencyRepository: AgencyRepository,
    private val crisisEpisodeTracker: CrisisEpisodeTracker,
    private val calculateAgencyIndexUseCase: CalculateAgencyIndexUseCase
) {
    suspend operator fun invoke(atEpochMillis: Long) {
        agencyRepository.reportAgencyState(AgencyState.Stable)
        val delta = crisisEpisodeTracker.onAgencyStateUpdated(AgencyState.Stable, atEpochMillis)
        if (!delta.isEmpty) {
            calculateAgencyIndexUseCase(delta.deltaAutonomy, delta.deltaSupport)
        }
    }
}
