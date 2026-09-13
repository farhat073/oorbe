package com.oorbitt.launcher.data.repository

import com.oorbitt.launcher.model.GestureAction
import kotlinx.coroutines.flow.Flow

interface GestureRepository {
    fun getAllGestureActions(): Flow<List<GestureAction>>
    suspend fun setGestureAction(gestureAction: GestureAction)
    suspend fun clearAll()
}
