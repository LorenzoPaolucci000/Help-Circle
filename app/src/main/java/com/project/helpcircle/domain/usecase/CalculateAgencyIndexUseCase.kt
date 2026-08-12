package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.repository.AgencyRepository

/**
 * Applies a Delta_Autonomy and/or Delta_Support adjustment to the running totals behind IA_ind
 * (e.g. +[com.project.helpcircle.domain.engine.DetectionConfig.SPONTANEOUS_RECOVERY_DELTA] for a
 * spontaneous recovery), and persists the recomputed index via [AgencyRepository].
 */
class CalculateAgencyIndexUseCase(
    private val agencyRepository: AgencyRepository
) {
    suspend operator fun invoke(deltaAutonomy: Int = 0, deltaSupport: Int = 0): AgencyIndex =
        agencyRepository.adjustAgencyDeltas(deltaAutonomy, deltaSupport)
}
