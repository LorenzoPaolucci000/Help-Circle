package com.project.helpcircle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.helpcircle.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

/** Room DAO for persisted focus sessions. */
@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions ORDER BY startedAtEpochMillis DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>
}
