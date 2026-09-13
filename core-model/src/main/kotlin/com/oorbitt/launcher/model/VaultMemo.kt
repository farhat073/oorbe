package com.oorbitt.launcher.model

data class VaultMemo(
    val id: Long = 0,
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val color: MemoColor = MemoColor.DEFAULT,
    val blocks: List<MemoBlock> = emptyList()
)

enum class MemoColor(val hexValue: String) {
    DEFAULT("#1E1E2E"),
    RED("#F38BA8"),
    ORANGE("#FAB387"),
    YELLOW("#F9E2AF"),
    GREEN("#A6E3A1"),
    BLUE("#89B4FA"),
    PURPLE("#CBA6F7"),
    PINK("#F5C2E7")
}

sealed class MemoBlock {
    abstract val id: String

    data class Text(
        override val id: String,
        val markdown: String
    ) : MemoBlock()

    data class Audio(
        override val id: String,
        val encryptedFileUri: String,
        val durationMs: Long,
        val transcription: String? = null
    ) : MemoBlock()

    data class Drawing(
        override val id: String,
        val encryptedFileUri: String,
        val strokeData: String
    ) : MemoBlock()

    data class Photo(
        override val id: String,
        val encryptedFileUri: String,
        val thumbnailUri: String? = null
    ) : MemoBlock()

    data class FileAttachment(
        override val id: String,
        val encryptedFileUri: String,
        val originalName: String,
        val mimeType: String,
        val sizeBytes: Long
    ) : MemoBlock()

    data class Checklist(
        override val id: String,
        val items: List<ChecklistItem>
    ) : MemoBlock()
}

data class ChecklistItem(
    val text: String,
    val checked: Boolean = false
)
