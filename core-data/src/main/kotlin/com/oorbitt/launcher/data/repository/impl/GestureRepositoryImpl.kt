package com.oorbitt.launcher.data.repository.impl

import com.oorbitt.launcher.data.db.dao.GestureMapDao
import com.oorbitt.launcher.data.db.entity.GestureMapEntity
import com.oorbitt.launcher.data.repository.GestureRepository
import com.oorbitt.launcher.model.GestureAction
import com.oorbitt.launcher.model.GestureType
import com.oorbitt.launcher.model.LauncherAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GestureRepositoryImpl(
    private val gestureMapDao: GestureMapDao
) : GestureRepository {

    private val defaultMappings = mapOf(
        GestureType.SWIPE_UP to LauncherAction.OPEN_DRAWER,
        GestureType.SWIPE_DOWN to LauncherAction.OPEN_NOTIFICATIONS,
        GestureType.DOUBLE_TAP to LauncherAction.NONE,
        GestureType.PINCH_IN to LauncherAction.NONE,
        GestureType.PINCH_OUT to LauncherAction.NONE,
        GestureType.SWIPE_UP_TWO_FINGER to LauncherAction.OPEN_SETTINGS,
        GestureType.LONG_PRESS_EMPTY to LauncherAction.OPEN_SETTINGS,
        GestureType.SWIPE_LEFT to LauncherAction.NONE,
        GestureType.SWIPE_RIGHT to LauncherAction.NONE
    )

    override fun getAllGestureActions(): Flow<List<GestureAction>> {
        return gestureMapDao.getAll().map { entities ->
            val entityMap = entities.associateBy { 
                try { GestureType.valueOf(it.gestureType) } catch (e: Exception) { null }
            }
            GestureType.entries.map { type ->
                val entity = entityMap[type]
                if (entity != null) {
                    GestureAction(
                        gestureType = type,
                        action = try { LauncherAction.valueOf(entity.action) } catch (e: Exception) { LauncherAction.NONE },
                        targetPackage = entity.targetPackage,
                        targetActivity = entity.targetActivity
                    )
                } else {
                    GestureAction(
                        gestureType = type,
                        action = defaultMappings[type] ?: LauncherAction.NONE
                    )
                }
            }
        }
    }

    override suspend fun setGestureAction(gestureAction: GestureAction) {
        gestureMapDao.insertOrUpdate(
            GestureMapEntity(
                gestureType = gestureAction.gestureType.name,
                action = gestureAction.action.name,
                targetPackage = gestureAction.targetPackage,
                targetActivity = gestureAction.targetActivity
            )
        )
    }

    override suspend fun clearAll() {
        gestureMapDao.deleteAll()
    }
}
