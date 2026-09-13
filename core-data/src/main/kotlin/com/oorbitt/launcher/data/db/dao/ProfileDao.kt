package com.oorbitt.launcher.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.oorbitt.launcher.data.db.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles")
    fun getAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE is_active = 1 LIMIT 1")
    fun getActive(): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProfileEntity): Long

    @Update
    suspend fun update(entity: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE profiles SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE profiles SET is_active = 1 WHERE id = :id")
    suspend fun activateById(id: Long)

    @Transaction
    suspend fun setActive(id: Long) {
        deactivateAll()
        activateById(id)
    }
}
