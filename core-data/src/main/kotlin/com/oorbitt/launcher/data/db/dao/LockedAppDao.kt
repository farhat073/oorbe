package com.oorbitt.launcher.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oorbitt.launcher.data.db.entity.LockedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {

    @Query("SELECT * FROM locked_apps")
    fun getAll(): Flow<List<LockedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LockedAppEntity)

    @Query("DELETE FROM locked_apps WHERE componentKey = :key")
    suspend fun deleteByKey(key: String)
}
