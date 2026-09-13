package com.oorbitt.launcher.model

data class DrawerGroup(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val appPackageNames: List<String> = emptyList(),
    val spanX: Int = 2,
    val spanY: Int = 1,
    val bgColor: String = "#22FFFFFF",
    val cornerRadiusDp: Int = 16,
    val textColor: String = "#FFFFFF"
) {
    fun toSerializedString(): String {
        val apps = appPackageNames.joinToString(",")
        val escapedTitle = title.replace(";", "").replace("|", "")
        return "$id;$escapedTitle;$apps;$spanX;$spanY;$bgColor;$cornerRadiusDp;$textColor"
    }

    companion object {
        fun fromSerializedString(str: String): DrawerGroup? {
            val parts = str.split(";")
            if (parts.size < 8) return null
            return try {
                DrawerGroup(
                    id = parts[0],
                    title = parts[1],
                    appPackageNames = if (parts[2].isBlank()) emptyList() else parts[2].split(","),
                    spanX = parts[3].toIntOrNull() ?: 2,
                    spanY = parts[4].toIntOrNull() ?: 1,
                    bgColor = parts[5],
                    cornerRadiusDp = parts[6].toIntOrNull() ?: 16,
                    textColor = parts[7]
                )
            } catch (e: Exception) {
                null
            }
        }

        fun toJsonString(groups: List<DrawerGroup>): String {
            // Persist as a delimited string safely
            return groups.joinToString("|") { it.toSerializedString() }
        }

        fun fromJsonString(jsonStr: String?): List<DrawerGroup> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            return jsonStr.split("|").mapNotNull { fromSerializedString(it) }
        }
    }
}
