package com.oorbitt.launcher.model

data class SearchResult(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val iconUri: String? = null,
    val action: SearchResultAction,
    val relevanceScore: Float = 0f,
    val providerName: String = ""
)

sealed class SearchResultAction {
    data class LaunchApp(val packageName: String, val activityName: String) : SearchResultAction()
    data class OpenUrl(val url: String) : SearchResultAction()
    data class ShowAnswer(val answer: String) : SearchResultAction()
    data class OpenSetting(val settingAction: String) : SearchResultAction()
    data class ExecuteCommand(val command: String) : SearchResultAction()
    data class LaunchShortcut(val shortcutId: String, val packageName: String) : SearchResultAction()
    data class CallContact(val phoneNumber: String) : SearchResultAction()
    data class MessageContact(val contactUri: String) : SearchResultAction()
    data class OpenFile(val uri: String, val mimeType: String) : SearchResultAction()
}
