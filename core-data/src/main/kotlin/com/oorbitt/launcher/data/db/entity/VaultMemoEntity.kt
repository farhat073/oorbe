package com.oorbitt.launcher.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_memos")
data class VaultMemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean,
    val color: String,
    @ColumnInfo(name = "blocks_json") val blocksJson: String, // JSON-serialized list of MemoBlock
)
