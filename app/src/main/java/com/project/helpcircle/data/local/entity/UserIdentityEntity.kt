package com.project.helpcircle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room row persisting the local user's anonymous identity. Single row keyed by [SINGLETON_ID] since there's one identity per device. */
@Entity(tableName = "user_identity")
data class UserIdentityEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val anonymousHash: String,
    val nickname: String = ""
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
