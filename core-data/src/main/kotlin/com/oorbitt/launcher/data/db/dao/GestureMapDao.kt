package com.oorbitt.launcher.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oorbitt.launcher.data.db.entity.GestureMapEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureMapDao {

    @Query("SELECT * FROM gesture_map")
    fun getAll(): Flow<List<GestureMapEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: GestureMapEntity)

    @Query("DELETE FROM gesture_map")
    suspend fun deleteAll()
}
