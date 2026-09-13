package com.oorbitt.launcher.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_items")
data class WorkspaceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "item_type") val itemType: String,
    @ColumnInfo(name = "container_type") val containerType: String,
    @ColumnInfo(name = "screen_index") val screenIndex: Int,
    @ColumnInfo(name = "cell_x") val cellX: Int,
    @ColumnInfo(name = "cell_y") val cellY: Int,
    @ColumnInfo(name = "span_x") val spanX: Int,
    @ColumnInfo(name = "span_y") val spanY: Int,
    @ColumnInfo(name = "package_name") val packageName: String?,
    @ColumnInfo(name = "activity_name") val activityName: String?,
    @ColumnInfo(name = "app_widget_id") val appWidgetId: Int,
    @ColumnInfo(name = "folder_id") val folderId: Long?,
    @ColumnInfo(name = "folder_title") val folderTitle: String?,
    @ColumnInfo(name = "dock_index") val dockIndex: Int,
    @ColumnInfo(name = "profile_id") val profileId: Long,
)
