package com.project.helpcircle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.helpcircle.data.local.entity.WeeklySatisfactionEntity
import kotlinx.coroutines.flow.Flow

/** Room DAO for the local-only history of this device's own weekly satisfaction ratings. */
@Dao
interface WeeklySatisfactionDao {
    /** REPLACE rather than IGNORE: changing your mind about the current week must overwrite the earlier answer. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeeklySatisfactionEntity)

    @Query("SELECT * FROM weekly_satisfaction WHERE weekStartEpochMillis = :weekStartEpochMillis")
    fun observeForWeek(weekStartEpochMillis: Long): Flow<WeeklySatisfactionEntity?>
}
