package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityWeeklyTrend
import com.project.helpcircle.domain.repository.CommunityWeeklyHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Streams this device's locally-recorded IA_comm history for a community, reduced to the latest and previous weekly snapshots for the Community tab's "vs. last week" comparison. */
class ObserveCommunityWeeklyTrendUseCase(
    private val communityWeeklyHistoryRepository: CommunityWeeklyHistoryRepository
) {
    operator fun invoke(communityId: String): Flow<CommunityWeeklyTrend> =
        communityWeeklyHistoryRepository.weeklySummaries(communityId).map { summaries ->
            val oldestFirst = summaries.sortedBy { it.weekStartEpochMillis }
            CommunityWeeklyTrend(
                latest = oldestFirst.lastOrNull(),
                previous = oldestFirst.dropLast(1).lastOrNull()
            )
        }
}
