package com.project.helpcircle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.helpcircle.data.local.entity.UserIdentityEntity

/** Room DAO for the single-row anonymous user identity. */
@Dao
interface UserIdentityDao {
    @Query("SELECT * FROM user_identity WHERE id = ${UserIdentityEntity.SINGLETON_ID}")
    suspend fun get(): UserIdentityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(identity: UserIdentityEntity)
}
