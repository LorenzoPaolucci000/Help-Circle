package com.project.helpcircle.data.repository

import com.project.helpcircle.data.local.dao.CommunityWeeklyHistoryDao
import com.project.helpcircle.data.local.entity.CommunityWeeklySummaryEntity
import com.project.helpcircle.domain.engine.WeeklyResetCalculator
import com.project.helpcircle.domain.model.CommunityWeeklySummary
import com.project.helpcircle.domain.repository.CommunityWeeklyHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed [CommunityWeeklyHistoryRepository]: persists this device's own IA_comm snapshots locally, never synced. */
class CommunityWeeklyHistoryRepositoryImpl @Inject constructor(
    private val communityWeeklyHistoryDao: CommunityWeeklyHistoryDao
) : CommunityWeeklyHistoryRepository {

    override fun weeklySummaries(communityId: String): Flow<List<CommunityWeeklySummary>> =
        communityWeeklyHistoryDao.observeSummaries(communityId).map { entities ->
            entities.map { CommunityWeeklySummary(it.communityId, it.weekStartEpochMillis, it.collectiveIndexValue) }
        }

    override suspend fun ensureWeeklySnapshotApplied(communityId: String, currentCollectiveIndexValue: Int) {
        val now = System.currentTimeMillis()
        val latestRecordedWeekStart = communityWeeklyHistoryDao.getLatestWeekStartEpochMillis(communityId)
        if (!WeeklyResetCalculator.shouldRecordNewWeeklySnapshot(latestRecordedWeekStart, now)) return
        communityWeeklyHistoryDao.insertSummary(
            CommunityWeeklySummaryEntity(
                communityId,
                WeeklyResetCalculator.mostRecentWeekStartEpochMillis(now),
                currentCollectiveIndexValue
            )
        )
    }
}
