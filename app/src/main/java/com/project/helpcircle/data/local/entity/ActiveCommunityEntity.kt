package com.project.helpcircle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room row persisting which community this device is currently a member of. Single row keyed by [SINGLETON_ID] since a device belongs to one community at a time. */
@Entity(tableName = "active_community")
data class ActiveCommunityEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val communityId: String
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
