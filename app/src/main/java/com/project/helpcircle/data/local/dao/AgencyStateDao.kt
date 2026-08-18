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

    /**
     * One-shot read of the same row [observe] exposes. Callers that just need the current value in
     * order to compute the next one must use this rather than `observe().firstOrNull()`: the latter
     * registers an invalidation observer and tears it down again for every single read, which is
     * pure overhead when nobody is watching for changes — and it is called from the accessibility
     * service's per-scroll path, where that overhead repeats for every scroll event.
     */
    @Query("SELECT * FROM agency_state WHERE id = ${AgencyStateEntity.SINGLETON_ID}")
    suspend fun get(): AgencyStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AgencyStateEntity)
}
