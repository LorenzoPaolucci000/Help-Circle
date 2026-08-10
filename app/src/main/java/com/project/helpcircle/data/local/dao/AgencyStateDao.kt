package com.project.helpcircle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.helpcircle.data.local.entity.AgencyStateEntity
import kotlinx.coroutines.flow.Flow

/** Room DAO for the single-row current agency index/state. */
@Dao
interface AgencyStateDao {
    @Query("SELECT * FROM agency_state WHERE id = ${AgencyStateEntity.SINGLETON_ID}")
    fun observe(): Flow<AgencyStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AgencyStateEntity)
}
