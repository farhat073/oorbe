package com.oorbitt.launcher.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oorbitt.launcher.data.repository.AppRepository
import com.oorbitt.launcher.data.repository.GestureRepository
import com.oorbitt.launcher.model.AppInfo
import com.oorbitt.launcher.model.GestureAction
import com.oorbitt.launcher.model.GestureType
import com.oorbitt.launcher.model.LauncherAction
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSettingsScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    gestureRepository: GestureRepository = koinInject(),
    appRepository: AppRepository = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val gestureActions by gestureRepository.getAllGestureActions().collectAsState(initial = emptyList())
    val allApps by appRepository.allApps.collectAsState(initial = emptyList())

    var selectedGestureForEdit by remember { mutableStateOf<GestureAction?>(null) }
    var showAppPickerForGesture by remember { mutableStateOf<GestureAction?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Gestures", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(gestureActions) { gestureAction ->
                val subtitle = when (gestureAction.action) {
                    LauncherAction.LAUNCH_APP -> {
                        val app = allApps.firstOrNull { it.packageName == gestureAction.targetPackage }
                        "Launch App: ${app?.displayLabel ?: gestureAction.targetPackage ?: "Select App"}"
                    }
                    LauncherAction.NONE -> "None (Disabled)"
                    else -> gestureAction.action.name.replace('_', ' ').lowercase().capitalizeWords()
                }
                
                ListItem(
                    headlineContent = {
                        Text(
                            text = gestureAction.gestureType.name.replace('_', ' ').lowercase().capitalizeWords(),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = subtitle,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .clickable { selectedGestureForEdit = gestureAction }
                        .padding(horizontal = 4.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                )
            }
        }
    }

    // Action Selection Sheet
    selectedGestureForEdit?.let { gestureAction ->
        ModalBottomSheet(
            onDismissRequest = { selectedGestureForEdit = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Select action for ${gestureAction.gestureType.name.replace('_', ' ').lowercase().capitalizeWords()}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
                HorizontalDivider()

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    val selectableActions = listOf(
                        LauncherAction.NONE to "None (Disabled)",
                        LauncherAction.OPEN_DRAWER to "Open App Drawer",
                        LauncherAction.OPEN_SEARCH to "Open Unified Search",
                        LauncherAction.OPEN_SETTINGS to "Open Launcher Settings",
                        LauncherAction.OPEN_NOTIFICATIONS to "Open Notification Shade",
                        LauncherAction.OPEN_QUICK_SETTINGS to "Open Quick Settings Panel",
                        LauncherAction.TOGGLE_TORCH to "Toggle Flashlight",
                        LauncherAction.OPEN_ORB_SPACE to "Open OrbSpace Dashboard",
                        LauncherAction.OPEN_ORB_SEARCH to "Open OrbSearch Overlay",
                        LauncherAction.LAUNCH_APP to "Launch Specific App"
                    )

                    items(selectableActions) { (actionType, actionLabel) ->
                        ListItem(
                            headlineContent = { Text(actionLabel) },
                            modifier = Modifier.clickable {
                                selectedGestureForEdit = null
                                if (actionType == LauncherAction.LAUNCH_APP) {
                                    showAppPickerForGesture = gestureAction
                                } else {
                                    coroutineScope.launch {
                                        gestureRepository.setGestureAction(gestureAction.copy(action = actionType))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // App Picker Sheet
    showAppPickerForGesture?.let { gestureAction ->
        ModalBottomSheet(
            onDismissRequest = { showAppPickerForGesture = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Select App to Launch",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()

                val sortedApps = remember(allApps) {
                    allApps.sortedBy { it.displayLabel.lowercase() }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(sortedApps) { app ->
                        ListItem(
                            headlineContent = { Text(app.displayLabel) },
                            supportingContent = { Text(app.packageName, fontSize = 11.sp) },
                            modifier = Modifier.clickable {
                                showAppPickerForGesture = null
                                coroutineScope.launch {
                                    gestureRepository.setGestureAction(
                                        gestureAction.copy(
                                            action = LauncherAction.LAUNCH_APP,
                                            targetPackage = app.packageName,
                                            targetActivity = app.activityName
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun String.capitalizeWords(): String =
    split(' ').joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
