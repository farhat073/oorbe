package com.oorbitt.launcher.data.repository.impl

import com.oorbitt.launcher.data.db.dao.WorkspaceDao
import com.oorbitt.launcher.data.db.entity.WorkspaceItemEntity
import com.oorbitt.launcher.data.repository.WorkspaceRepository
import com.oorbitt.launcher.model.ContainerType
import com.oorbitt.launcher.model.ItemType
import com.oorbitt.launcher.model.WorkspaceItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkspaceRepositoryImpl(
    private val workspaceDao: WorkspaceDao
) : WorkspaceRepository {

    override fun getItemsForProfile(profileId: Long): Flow<List<WorkspaceItem>> =
        workspaceDao.getItemsForProfile(profileId).map { entities ->
            entities.map { it.toModel() }
        }

    override fun getDockItems(profileId: Long): Flow<List<WorkspaceItem>> =
        workspaceDao.getDockItems(profileId).map { entities ->
            entities.map { it.toModel() }
        }

    override fun getFolderItems(folderId: Long): Flow<List<WorkspaceItem>> =
        workspaceDao.getFolderItems(folderId).map { entities ->
            entities.map { it.toModel() }
        }

    override suspend fun addItem(item: WorkspaceItem): Long =
        workspaceDao.insert(item.toEntity())

    override suspend fun updateItem(item: WorkspaceItem) =
        workspaceDao.update(item.toEntity())

    override suspend fun removeItem(id: Long) =
        workspaceDao.deleteById(id)

    override suspend fun replaceAllForProfile(profileId: Long, items: List<WorkspaceItem>) =
        workspaceDao.replaceAllForProfile(profileId, items.map { it.toEntity() })

    private fun WorkspaceItemEntity.toModel() = WorkspaceItem(
        id = id,
        itemType = ItemType.valueOf(itemType),
        containerType = ContainerType.valueOf(containerType),
        screenIndex = screenIndex,
        cellX = cellX,
        cellY = cellY,
        spanX = spanX,
        spanY = spanY,
        packageName = packageName,
        activityName = activityName,
        appWidgetId = appWidgetId,
        folderId = folderId,
        folderTitle = folderTitle,
        dockIndex = dockIndex,
        profileId = profileId
    )

    private fun WorkspaceItem.toEntity() = WorkspaceItemEntity(
        id = id,
        itemType = itemType.name,
        containerType = containerType.name,
        screenIndex = screenIndex,
        cellX = cellX,
        cellY = cellY,
        spanX = spanX,
        spanY = spanY,
        packageName = packageName,
        activityName = activityName,
        appWidgetId = appWidgetId,
        folderId = folderId,
        folderTitle = folderTitle,
        dockIndex = dockIndex,
        profileId = profileId
    )
}
