package com.oorbitt.launcher.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oorbitt.launcher.data.db.entity.HiddenAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenAppDao {

    @Query("SELECT * FROM hidden_apps")
    fun getAll(): Flow<List<HiddenAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HiddenAppEntity)

    @Delete
    suspend fun delete(entity: HiddenAppEntity)

    @Query("DELETE FROM hidden_apps WHERE componentKey = :key")
    suspend fun deleteByKey(key: String)
}
