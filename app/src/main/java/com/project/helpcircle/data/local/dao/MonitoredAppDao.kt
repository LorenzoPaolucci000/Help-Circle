package com.project.helpcircle.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.helpcircle.data.local.entity.MonitoredAppEntity
import kotlinx.coroutines.flow.Flow

/** Room DAO for the user's monitored-apps blacklist. */
@Dao
interface MonitoredAppDao {
    @Query("SELECT packageName FROM monitored_apps")
    fun observePackageNames(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM monitored_apps WHERE packageName = :packageName)")
    suspend fun isMonitored(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MonitoredAppEntity)

    @Delete
    suspend fun delete(entity: MonitoredAppEntity)
}
