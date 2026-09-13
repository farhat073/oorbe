package com.oorbitt.launcher.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.oorbitt.launcher.data.db.entity.VaultMemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultMemoDao {

    @Query("SELECT * FROM vault_memos ORDER BY is_pinned DESC, updated_at DESC")
    fun getAll(): Flow<List<VaultMemoEntity>>

    @Query("SELECT * FROM vault_memos WHERE id = :id")
    fun getById(id: Long): Flow<VaultMemoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VaultMemoEntity): Long

    @Update
    suspend fun update(entity: VaultMemoEntity)

    @Query("DELETE FROM vault_memos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vault_memos")
    suspend fun deleteAll()
}
