package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.AgencyDetectionEngine
import com.project.helpcircle.domain.engine.ScrollSignal
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.repository.AgencyRepository

/** Feeds a scroll signal into [AgencyDetectionEngine] and reports the resulting [AgencyState]. */
class DetectLossOfAgencyUseCase(
    private val agencyDetectionEngine: AgencyDetectionEngine,
    private val agencyRepository: AgencyRepository
) {
    suspend operator fun invoke(signal: ScrollSignal): AgencyState {
        val state = agencyDetectionEngine.record(signal)
        agencyRepository.reportAgencyState(state)
        return state
    }
}
