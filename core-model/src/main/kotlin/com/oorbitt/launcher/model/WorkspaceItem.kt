package com.oorbitt.launcher.model

data class WorkspaceItem(
    val id: Long = 0,
    val itemType: ItemType,
    val containerType: ContainerType = ContainerType.WORKSPACE,
    val screenIndex: Int = 0,
    val cellX: Int = 0,
    val cellY: Int = 0,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val packageName: String? = null,
    val activityName: String? = null,
    val appWidgetId: Int = -1,
    val folderId: Long? = null,
    val folderTitle: String? = null,
    val dockIndex: Int = -1,
    val profileId: Long = 0
)

enum class ItemType {
    APP, WIDGET, FOLDER, SHORTCUT
}

enum class ContainerType {
    WORKSPACE, DOCK, FOLDER
}
