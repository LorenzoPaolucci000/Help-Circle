package com.project.helpcircle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row persisting the local user's current agency index/state, plus the running
 * Delta_Autonomy/Delta_Support totals the IA_ind formula is recomputed from on every adjustment,
 * and the weekly-reset bookkeeping ([lastArchivedAgencyIndexValue]/[lastWeeklyResetAtEpochMillis]).
 * Single row keyed by [SINGLETON_ID] since there's one live state per device.
 */
@Entity(tableName = "agency_state")
data class AgencyStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val agencyIndexValue: Int,
    val agencyState: String,
    val deltaAutonomy: Int = 0,
    val deltaSupport: Int = 0,
    val lastArchivedAgencyIndexValue: Int? = null,
    val lastWeeklyResetAtEpochMillis: Long? = null
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
