package com.project.helpcircle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room row persisting the local user's charge wallet. Single row keyed by [SINGLETON_ID] since there's one wallet per device. */
@Entity(tableName = "charge_wallet")
data class ChargeWalletEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val currentCharges: Int,
    val lastReplenishedAtEpochMillis: Long
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
