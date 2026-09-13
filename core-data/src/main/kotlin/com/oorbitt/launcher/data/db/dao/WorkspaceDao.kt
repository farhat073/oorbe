package com.oorbitt.launcher.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.oorbitt.launcher.data.db.entity.WorkspaceItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    @Query("SELECT * FROM workspace_items WHERE profile_id = :profileId ORDER BY screen_index, cell_y, cell_x")
    fun getItemsForProfile(profileId: Long): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_items WHERE container_type = 'DOCK' AND profile_id = :profileId ORDER BY dock_index")
    fun getDockItems(profileId: Long): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_items WHERE folder_id = :folderId")
    fun getFolderItems(folderId: Long): Flow<List<WorkspaceItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WorkspaceItemEntity): Long

    @Update
    suspend fun update(item: WorkspaceItemEntity)

    @Delete
    suspend fun delete(item: WorkspaceItemEntity)

    @Query("DELETE FROM workspace_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workspace_items WHERE profile_id = :profileId")
    suspend fun deleteAllForProfile(profileId: Long)

    @Transaction
    suspend fun replaceAllForProfile(profileId: Long, items: List<WorkspaceItemEntity>) {
        deleteAllForProfile(profileId)
        items.forEach { insert(it) }
    }
}
