package com.oorbitt.launcher.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.oorbitt.launcher.model.AppInfo
import com.oorbitt.launcher.model.WorkspaceItem

class DragManager {
    var activeDragItem by mutableStateOf<AppInfo?>(null)
    var activeWorkspaceDragItem by mutableStateOf<WorkspaceItem?>(null)
    var isDraggingFromDrawer by mutableStateOf(false)
    
    // Absolute position of the finger in window coordinates
    var touchPosition by mutableStateOf(Offset.Zero)
    
    // The offset of the finger relative to the top-left of the item
    var holdOffset by mutableStateOf(Offset.Zero)
    
    var dragCurrentCell by mutableStateOf<Pair<Int, Int>?>(null)
    var currentDragPage by mutableStateOf<Int?>(null)

    // The top-left corner of the item (for rendering the drag shadow)
    val itemPosition: Offset
        get() = touchPosition - holdOffset

    fun startDrawerDrag(app: AppInfo, itemPos: Offset, initialTouchOffset: Offset) {
        activeDragItem = app
        isDraggingFromDrawer = true
        holdOffset = initialTouchOffset
        touchPosition = itemPos + initialTouchOffset
        activeWorkspaceDragItem = null
        dragCurrentCell = null
        currentDragPage = null
    }

    fun startWorkspaceDrag(item: WorkspaceItem, itemPos: Offset, initialTouchOffset: Offset) {
        activeWorkspaceDragItem = item
        isDraggingFromDrawer = false
        holdOffset = initialTouchOffset
        touchPosition = itemPos + initialTouchOffset
        activeDragItem = null
        dragCurrentCell = Pair(item.cellX, item.cellY)
        currentDragPage = item.screenIndex
    }
    
    fun updateDrag(amount: Offset) {
        touchPosition += amount
    }
    
    fun endDrag() {
        activeDragItem = null
        activeWorkspaceDragItem = null
        isDraggingFromDrawer = false
        touchPosition = Offset.Zero
        holdOffset = Offset.Zero
        dragCurrentCell = null
        currentDragPage = null
    }
}

