package com.oorbitt.launcher.data.repository

import com.oorbitt.launcher.model.VaultMemo
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    fun getAllMemos(): Flow<List<VaultMemo>>
    fun getMemoById(id: Long): Flow<VaultMemo?>
    suspend fun saveMemo(memo: VaultMemo): Long
    suspend fun deleteMemo(id: Long)
    suspend fun clearAll()
}
