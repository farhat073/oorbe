package com.oorbitt.launcher.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oorbitt.launcher.data.db.entity.IconOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IconOverrideDao {

    @Query("SELECT * FROM icon_overrides")
    fun getAll(): Flow<List<IconOverrideEntity>>

    @Query("SELECT * FROM icon_overrides WHERE componentKey = :key")
    suspend fun getByKey(key: String): IconOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: IconOverrideEntity)

    @Delete
    suspend fun delete(entity: IconOverrideEntity)
}
