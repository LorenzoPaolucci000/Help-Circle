package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.AgencyHomeSummary
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.WeeklyHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Streams the live IA_ind alongside the locally-stored weekly history, for the personal "Me" home screen. */
class ObserveAgencyHomeUseCase(
    private val agencyRepository: AgencyRepository,
    private val weeklyHistoryRepository: WeeklyHistoryRepository
) {
    operator fun invoke(): Flow<AgencyHomeSummary> =
        combine(agencyRepository.currentAgencyIndex, weeklyHistoryRepository.weeklySummaries) { index, summaries ->
            AgencyHomeSummary(
                currentIndex = index,
                weeklySummariesOldestFirst = summaries.sortedBy { it.weekStartEpochMillis }
            )
        }
}
