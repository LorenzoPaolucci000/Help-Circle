package com.project.helpcircle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.helpcircle.data.local.entity.ActiveCommunityEntity

/** Room DAO for the single-row locally active community membership. */
@Dao
interface ActiveCommunityDao {
    @Query("SELECT * FROM active_community WHERE id = ${ActiveCommunityEntity.SINGLETON_ID}")
    suspend fun get(): ActiveCommunityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ActiveCommunityEntity)

    @Query("DELETE FROM active_community WHERE id = ${ActiveCommunityEntity.SINGLETON_ID}")
    suspend fun clear()
}
