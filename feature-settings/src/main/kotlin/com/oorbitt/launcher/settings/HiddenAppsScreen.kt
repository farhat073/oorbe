package com.oorbitt.launcher.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oorbitt.launcher.data.repository.AppRepository
import com.oorbitt.launcher.data.repository.SettingsRepository
import com.oorbitt.launcher.model.AppInfo
import com.oorbitt.launcher.model.LauncherSettings
import com.oorbitt.launcher.ui.util.rememberAppIcon
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAppsScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    appRepository: AppRepository = koinInject(),
    settingsRepository: SettingsRepository = koinInject()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allApps by appRepository.allApps.collectAsState(initial = emptyList())
    val settingsState by settingsRepository.settings.collectAsState(initial = LauncherSettings())
    var searchQuery by remember { mutableStateOf("") }

    val onAppToggle = remember(appRepository, coroutineScope) {
        { app: AppInfo, checked: Boolean ->
            coroutineScope.launch {
                if (checked) {
                    appRepository.hideApp(app.componentKey)
                } else {
                    appRepository.unhideApp(app.componentKey)
                }
            }
            Unit
        }
    }

    val onAppClick = remember(appRepository, coroutineScope) {
        { app: AppInfo ->
            coroutineScope.launch {
                if (app.isHidden) {
                    appRepository.unhideApp(app.componentKey)
                } else {
                    appRepository.hideApp(app.componentKey)
                }
            }
            Unit
        }
    }

    val filteredApps = remember(allApps, searchQuery) {
        val sorted = allApps.sortedBy { it.displayLabel.lowercase() }
        if (searchQuery.isBlank()) {
            sorted
        } else {
            sorted.filter { it.displayLabel.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Hidden Apps", fontWeight = FontWeight.Bold) },
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
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search apps...") },
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
                items(
                    items = filteredApps,
                    key = { it.componentKey }
                ) { app ->
                    HiddenAppListItem(
                        app = app,
                        activeIconPack = settingsState.activeIconPack,
                        onToggle = onAppToggle,
                        onClick = onAppClick
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

@Composable
private fun HiddenAppListItem(
    app: AppInfo,
    activeIconPack: String?,
    onToggle: (AppInfo, Boolean) -> Unit,
    onClick: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    ListItem(
        headlineContent = {
            Text(
                text = app.displayLabel,
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = {
            Text(
                text = if (app.isHidden) "Hidden from App Drawer" else "Visible in App Drawer",
                style = MaterialTheme.typography.bodySmall,
                color = if (app.isHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        leadingContent = {
            val icon = rememberAppIcon(context, app.packageName, app.activityName, activeIconPack)
            icon?.let {
                Image(
                    bitmap = it,
                    contentDescription = app.displayLabel,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            } ?: Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.5f))
            )
        },
        trailingContent = {
            Switch(
                checked = app.isHidden,
                onCheckedChange = { checked -> onToggle(app, checked) }
            )
        },
        modifier = Modifier
            .clickable { onClick(app) }
            .padding(horizontal = 4.dp)
    )
}
