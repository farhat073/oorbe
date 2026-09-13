package com.oorbitt.launcher.data.repository.impl

import android.util.Base64
import com.oorbitt.launcher.data.db.dao.VaultMemoDao
import com.oorbitt.launcher.data.db.entity.VaultMemoEntity
import com.oorbitt.launcher.data.repository.VaultRepository
import com.oorbitt.launcher.model.ChecklistItem
import com.oorbitt.launcher.model.MemoBlock
import com.oorbitt.launcher.model.MemoColor
import com.oorbitt.launcher.model.VaultMemo
import com.oorbitt.launcher.security.EncryptionEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class VaultRepositoryImpl(
    private val dao: VaultMemoDao,
    private val encryptionEngine: EncryptionEngine
) : VaultRepository {

    override fun getAllMemos(): Flow<List<VaultMemo>> {
        return dao.getAll().map { entities ->
            entities.map { decryptEntity(it) }
        }
    }

    override fun getMemoById(id: Long): Flow<VaultMemo?> {
        return dao.getById(id).map { entity ->
            entity?.let { decryptEntity(it) }
        }
    }

    override suspend fun saveMemo(memo: VaultMemo): Long {
        val entity = encryptMemo(memo)
        return if (entity.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            entity.id
        }
    }

    override suspend fun deleteMemo(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun clearAll() {
        dao.deleteAll()
    }

    // ---- encryption / decryption helpers ------------------------------------

    private fun encryptMemo(memo: VaultMemo): VaultMemoEntity {
        val rawTitle = memo.title
        val rawBlocksJson = serializeBlocks(memo.blocks)

        val encryptedTitle = try {
            val encryptedBytes = encryptionEngine.encrypt(rawTitle.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            rawTitle // fallback to plain if encryption fails
        }

        val encryptedBlocksJson = try {
            val encryptedBytes = encryptionEngine.encrypt(rawBlocksJson.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            rawBlocksJson // fallback to plain if encryption fails
        }

        return VaultMemoEntity(
            id = memo.id,
            title = encryptedTitle,
            createdAt = memo.createdAt,
            updatedAt = memo.updatedAt,
            isPinned = memo.isPinned,
            color = memo.color.name,
            blocksJson = encryptedBlocksJson
        )
    }

    private fun decryptEntity(entity: VaultMemoEntity): VaultMemo {
        val title = try {
            val encryptedBytes = Base64.decode(entity.title, Base64.DEFAULT)
            String(encryptionEngine.decrypt(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            // If it fails to decrypt (e.g. not encrypted yet or invalid key), return raw title
            entity.title
        }

        val blocksJson = try {
            val encryptedBytes = Base64.decode(entity.blocksJson, Base64.DEFAULT)
            String(encryptionEngine.decrypt(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            // Fallback to raw blocksJson
            entity.blocksJson
        }

        val blocks = deserializeBlocks(blocksJson)

        val color = try {
            MemoColor.valueOf(entity.color)
        } catch (e: Exception) {
            MemoColor.DEFAULT
        }

        return VaultMemo(
            id = entity.id,
            title = title,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isPinned = entity.isPinned,
            color = color,
            blocks = blocks
        )
    }

    // ---- serialization ------------------------------------------------------

    private fun serializeBlocks(blocks: List<MemoBlock>): String {
        val array = JSONArray()
        for (block in blocks) {
            val obj = JSONObject()
            obj.put("id", block.id)
            when (block) {
                is MemoBlock.Text -> {
                    obj.put("type", "text")
                    obj.put("markdown", block.markdown)
                }
                is MemoBlock.Audio -> {
                    obj.put("type", "audio")
                    obj.put("encryptedFileUri", block.encryptedFileUri)
                    obj.put("durationMs", block.durationMs)
                    obj.put("transcription", block.transcription)
                }
                is MemoBlock.Drawing -> {
                    obj.put("type", "drawing")
                    obj.put("encryptedFileUri", block.encryptedFileUri)
                    obj.put("strokeData", block.strokeData)
                }
                is MemoBlock.Photo -> {
                    obj.put("type", "photo")
                    obj.put("encryptedFileUri", block.encryptedFileUri)
                    obj.put("thumbnailUri", block.thumbnailUri)
                }
                is MemoBlock.FileAttachment -> {
                    obj.put("type", "file_attachment")
                    obj.put("encryptedFileUri", block.encryptedFileUri)
                    obj.put("originalName", block.originalName)
                    obj.put("mimeType", block.mimeType)
                    obj.put("sizeBytes", block.sizeBytes)
                }
                is MemoBlock.Checklist -> {
                    obj.put("type", "checklist")
                    val itemArray = JSONArray()
                    for (item in block.items) {
                        val itemObj = JSONObject()
                        itemObj.put("text", item.text)
                        itemObj.put("checked", item.checked)
                        itemArray.put(itemObj)
                    }
                    obj.put("items", itemArray)
                }
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeBlocks(jsonStr: String): List<MemoBlock> {
        val list = mutableListOf<MemoBlock>()
        if (jsonStr.isBlank()) return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                when (obj.getString("type")) {
                    "text" -> {
                        list.add(MemoBlock.Text(id, obj.getString("markdown")))
                    }
                    "audio" -> {
                        list.add(
                            MemoBlock.Audio(
                                id = id,
                                encryptedFileUri = obj.getString("encryptedFileUri"),
                                durationMs = obj.getLong("durationMs"),
                                transcription = if (obj.isNull("transcription")) null else obj.getString("transcription")
                            )
                        )
                    }
                    "drawing" -> {
                        list.add(
                            MemoBlock.Drawing(
                                id = id,
                                encryptedFileUri = obj.getString("encryptedFileUri"),
                                strokeData = obj.getString("strokeData")
                            )
                        )
                    }
                    "photo" -> {
                        list.add(
                            MemoBlock.Photo(
                                id = id,
                                encryptedFileUri = obj.getString("encryptedFileUri"),
                                thumbnailUri = if (obj.isNull("thumbnailUri")) null else obj.getString("thumbnailUri")
                            )
                        )
                    }
                    "file_attachment" -> {
                        list.add(
                            MemoBlock.FileAttachment(
                                id = id,
                                encryptedFileUri = obj.getString("encryptedFileUri"),
                                originalName = obj.getString("originalName"),
                                mimeType = obj.getString("mimeType"),
                                sizeBytes = obj.getLong("sizeBytes")
                            )
                        )
                    }
                    "checklist" -> {
                        val itemArray = obj.getJSONArray("items")
                        val items = mutableListOf<ChecklistItem>()
                        for (j in 0 until itemArray.length()) {
                            val itemObj = itemArray.getJSONObject(j)
                            items.add(
                                ChecklistItem(
                                    text = itemObj.getString("text"),
                                    checked = itemObj.getBoolean("checked")
                                )
                            )
                        }
                        list.add(MemoBlock.Checklist(id, items))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
