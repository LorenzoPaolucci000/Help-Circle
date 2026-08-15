package com.project.helpcircle.domain.repository

import com.project.helpcircle.domain.model.CommunityWeeklySummary
import kotlinx.coroutines.flow.Flow

/**
 * Local-only history of the IA_comm snapshots this device has recorded for a community, one per
 * weekly boundary. Kept separate from [CommunityRepository], which owns the live Firestore-backed
 * IA_comm itself, the same way the individual score's [WeeklyHistoryRepository] is kept separate
 * from [AgencyRepository].
 */
interface CommunityWeeklyHistoryRepository {
    fun weeklySummaries(communityId: String): Flow<List<CommunityWeeklySummary>>

    /**
     * Records a snapshot of [currentCollectiveIndexValue] for the most recently passed weekly
     * boundary, if this device hasn't already recorded one for it. Safe to call on every live
     * IA_comm update — it's a cheap local check, and only ever writes once per boundary.
     */
    suspend fun ensureWeeklySnapshotApplied(communityId: String, currentCollectiveIndexValue: Int)
}
