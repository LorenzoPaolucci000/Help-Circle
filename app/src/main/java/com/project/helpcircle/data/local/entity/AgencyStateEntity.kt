package com.project.helpcircle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room row persisting the local user's current agency index/state. Single row keyed by [SINGLETON_ID] since there's one live state per device. */
@Entity(tableName = "agency_state")
data class AgencyStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val agencyIndexValue: Int,
    val agencyState: String
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
