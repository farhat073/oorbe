package com.oorbitt.launcher.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oorbitt.launcher.data.repository.FeatureFlagsRepository
import com.oorbitt.launcher.model.Feature
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeatureFlagsScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    featureFlagsRepository: FeatureFlagsRepository = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val allFlags by featureFlagsRepository.allFlags().collectAsState(initial = emptyMap())
    var searchQuery by remember { mutableStateOf("") }

    // Grouping helper
    val groupedFeatures = remember(allFlags, searchQuery) {
        val filteredFeatures = if (searchQuery.isBlank()) {
            Feature.entries
        } else {
            Feature.entries.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }

        filteredFeatures.groupBy { feature ->
            when (feature) {
                Feature.SMART_SURFACES, Feature.DOCK, Feature.PAGE_INDICATORS, Feature.EDGE_PANEL, Feature.ONE_HAND_MODE -> "Home Screen"
                Feature.HIDDEN_APPS, Feature.WORK_PROFILE_SECTION, Feature.AUTO_CATEGORIZE, Feature.SUGGESTED_APPS -> "App Drawer"
                Feature.SEARCH_APPS, Feature.SEARCH_CALCULATOR, Feature.SEARCH_UNIT_CONVERTER, Feature.SEARCH_CONTACTS,
                Feature.SEARCH_SETTINGS, Feature.SEARCH_SHORTCUTS, Feature.SEARCH_WEB_FALLBACK, Feature.COMMAND_BAR -> "Unified Search"
                Feature.ICON_PACKS, Feature.CUSTOM_ICON_SHAPES, Feature.MATERIAL_YOU, Feature.WALLPAPER_PARALLAX, Feature.CUSTOM_TRANSITIONS -> "Theme & Design"
                Feature.GESTURE_SWIPE_UP, Feature.GESTURE_SWIPE_DOWN, Feature.GESTURE_DOUBLE_TAP, Feature.GESTURE_PINCH, Feature.GESTURE_LONG_PRESS -> "Gestures"
                Feature.NOTIFICATION_BADGES, Feature.NOTIFICATION_PREVIEW -> "Notifications"
                Feature.SHIZUKU_FREEZE, Feature.SHIZUKU_FORCE_STOP, Feature.SHIZUKU_CLEAR_CACHE, Feature.SHIZUKU_FLOATING_WINDOW, Feature.SHIZUKU_RECENT_TASKS -> "Shizuku Integration"
                Feature.APP_DIET, Feature.APP_HYGIENE, Feature.CLIPBOARD_HISTORY -> "System Intelligence"
                Feature.GHOST_MODE, Feature.APP_LOCK, Feature.SECURE_VAULT -> "Privacy & Security"
                Feature.LAUNCHER_PROFILES -> "Profiles"
                Feature.LOCAL_BACKUP, Feature.COMMUNITY_SHARING -> "Backup & Sharing"
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Feature Flags", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search features...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                groupedFeatures.forEach { (category, features) ->
                    stickyHeader {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    items(features) { feature ->
                        val isEnabled = allFlags[feature] ?: feature.defaultEnabled
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = feature.name.replace('_', ' ').lowercase().capitalizeWords(),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = feature.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            featureFlagsRepository.setEnabled(feature, checked)
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .clickable {
                                    coroutineScope.launch {
                                        featureFlagsRepository.setEnabled(feature, !isEnabled)
                                    }
                                }
                                .padding(horizontal = 4.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }
            }
        }
    }
}

// Utility string extension
private fun String.capitalizeWords(): String =
    split(' ').joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
