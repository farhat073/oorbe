package com.oorbitt.launcher.data.repository

import com.oorbitt.launcher.model.WorkspaceItem
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    fun getItemsForProfile(profileId: Long): Flow<List<WorkspaceItem>>
    fun getDockItems(profileId: Long): Flow<List<WorkspaceItem>>
    fun getFolderItems(folderId: Long): Flow<List<WorkspaceItem>>
    suspend fun addItem(item: WorkspaceItem): Long
    suspend fun updateItem(item: WorkspaceItem)
    suspend fun removeItem(id: Long)
    suspend fun replaceAllForProfile(profileId: Long, items: List<WorkspaceItem>)
}
