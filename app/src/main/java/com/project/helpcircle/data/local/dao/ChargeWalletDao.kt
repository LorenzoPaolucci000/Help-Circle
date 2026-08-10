package com.project.helpcircle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.helpcircle.data.local.entity.ChargeWalletEntity
import kotlinx.coroutines.flow.Flow

/** Room DAO for the single-row charge wallet. */
@Dao
interface ChargeWalletDao {
    @Query("SELECT * FROM charge_wallet WHERE id = ${ChargeWalletEntity.SINGLETON_ID}")
    fun observe(): Flow<ChargeWalletEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wallet: ChargeWalletEntity)
}
