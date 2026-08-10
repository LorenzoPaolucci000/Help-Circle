package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.repository.AgencyRepository

/** Recomputes IA_ind from the baseline formula and persists it via [AgencyRepository]. */
class CalculateAgencyIndexUseCase(
    private val agencyRepository: AgencyRepository
) {
    suspend operator fun invoke(deltaAutonomy: Int, deltaSupport: Int): AgencyIndex {
        val index = AgencyIndex.calculate(deltaAutonomy, deltaSupport)
        agencyRepository.updateAgencyIndex(index)
        return index
    }
}
