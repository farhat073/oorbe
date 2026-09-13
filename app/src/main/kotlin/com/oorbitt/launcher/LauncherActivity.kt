package com.oorbitt.launcher

import android.content.Intent
import android.app.role.RoleManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import android.app.Activity
import android.view.WindowManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.oorbitt.launcher.drawer.DrawerScreen
import com.oorbitt.launcher.home.HomeScreen
import com.oorbitt.launcher.settings.SettingsScreen
import com.oorbitt.launcher.search.SearchScreen
import com.oorbitt.launcher.vault.VaultScreen
import com.oorbitt.launcher.orbspace.OrbSearchSurface
import com.oorbitt.launcher.security.AuthManagerImpl
import com.oorbitt.launcher.ui.theme.OorbittTheme
import com.oorbitt.launcher.data.repository.SettingsRepository
import com.oorbitt.launcher.model.LauncherSettings
import com.oorbitt.launcher.model.ThemeMode
import org.koin.compose.koinInject
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.android.ext.android.inject
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

fun Activity.enableHighRefreshRate() {
    // Ensure hardware acceleration
    window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
        val modes = display?.supportedModes ?: return
        val maxMode = modes.maxByOrNull { it.refreshRate }
        if (maxMode != null) {
            val params = window.attributes
            params.preferredDisplayModeId = maxMode.modeId
            window.attributes = params
        }
    }
}

class LauncherActivity : AppCompatActivity() {

    private val authManager: AuthManagerImpl by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var screenOffReceiver: BroadcastReceiver? = null

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Role request result */ }

    private val onboardingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            lifecycleScope.launch {
                settingsRepository.updateSettings { it.copy(hasCompletedOnboarding = true) }
                requestDefaultLauncher()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableHighRefreshRate()
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        registerScreenOffReceiver()

        val hasSeenOnboarding = kotlinx.coroutines.runBlocking {
            settingsRepository.settings.firstOrNull()?.hasCompletedOnboarding ?: false
        }
        if (!hasSeenOnboarding) {
            val intent = Intent().setClassName(this, "com.oorbitt.launcher.onboarding.OnboardingActivity")
            onboardingLauncher.launch(intent)
        } else {
            requestDefaultLauncher()
        }

        setContent {
            val settingsRepository: SettingsRepository = koinInject()
            val settingsState by settingsRepository.settings.collectAsState(initial = LauncherSettings())
            val isDark = when (settingsState.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            OorbittTheme(darkTheme = isDark) {
                var showOverlay by remember { mutableStateOf(false) }
                var showSearch by remember { mutableStateOf(false) }
                var showSettings by remember { mutableStateOf(false) }
                var settingsCategory by remember { mutableStateOf<String?>(null) }
                var showVault by remember { mutableStateOf(false) }
                var showOrbSearch by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                val initialPage = remember(settingsState.enableOrbSpace) { if (settingsState.enableOrbSpace) 1 else 0 }
                val pageCount = remember(settingsState.enableOrbSpace) { if (settingsState.enableOrbSpace) 2 else 1 }
                val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                    initialPage = initialPage,
                    pageCount = { pageCount }
                )

                LaunchedEffect(Unit) {
                    homePresses.collect {
                        showOverlay = false
                        showSettings = false
                        showSearch = false
                        showVault = false
                        showOrbSearch = false
                    }
                }

                BackHandler(enabled = showOverlay || showSettings || showSearch || showVault || showOrbSearch) {
                    if (showOrbSearch) {
                        showOrbSearch = false
                    } else if (showVault) {
                        showVault = false
                    } else if (showSearch) {
                        showSearch = false
                    } else if (showSettings) {
                        showSettings = false
                    } else {
                        // If we are in OrbSpace, go back to drawer first
                        if (settingsState.enableOrbSpace && pagerState.currentPage == 0) {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        } else {
                            showOverlay = false
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Home screen (always visible behind drawer)
                    HomeScreen(
                        isOverlayVisible = showOverlay,
                        onOpenDrawer = { 
                            scope.launch { pagerState.scrollToPage(1) }
                            showOverlay = true 
                        },
                        onOpenSearch = { showSearch = true },
                        onOpenSettings = { category ->
                            settingsCategory = category
                            showSettings = true
                        },
                        onOpenVault = {
                            scope.launch {
                                val authenticated = authManager.authenticate(
                                    title = "Secure Vault",
                                    subtitle = "Authenticate to access your secure memos"
                                )
                                if (authenticated) {
                                    showVault = true
                                }
                            }
                        },
                        onOpenOrbSpace = { 
                            scope.launch { pagerState.scrollToPage(0) }
                            showOverlay = true 
                        },
                        onOpenOrbSearch = { showOrbSearch = true },
                        modifier = Modifier.fillMaxSize()
                    )

                    val enterTransition = remember(settingsState.drawerAnimationType) {
                        when (settingsState.drawerAnimationType) {
                            "FADE" -> fadeIn(animationSpec = tween(300))
                            "ZOOM" -> scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = tween(300))
                            "CARD" -> slideInVertically(initialOffsetY = { it / 2 }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + scaleIn(initialScale = 0.95f) + fadeIn()
                            else -> slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn()
                        }
                    }
                    val exitTransition = remember(settingsState.drawerAnimationType) {
                        when (settingsState.drawerAnimationType) {
                            "FADE" -> fadeOut(animationSpec = tween(250))
                            "ZOOM" -> scaleOut(targetScale = 0.85f, animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
                            "CARD" -> slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(250)) + scaleOut(targetScale = 0.95f) + fadeOut()
                            else -> slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut()
                        }
                    }

                    // Drawer & OrbSpace overlay
                    AnimatedVisibility(
                        visible = showOverlay,
                        enter = enterTransition,
                        exit = exitTransition
                    ) {
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val actualPage = if (settingsState.enableOrbSpace) page else page + 1
                            when (actualPage) {
                                0 -> {
                                    com.oorbitt.launcher.orbspace.OrbSpaceScreen(
                                        isPageVisible = pagerState.currentPage == 0 && showOverlay
                                    )
                                }
                                1 -> {
                                    DrawerScreen(
                                        onDismiss = { showOverlay = false },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // Settings overlay
                    AnimatedVisibility(
                        visible = showSettings,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeOut(animationSpec = tween(300))
                    ) {
                        SettingsScreen(
                            initialCategory = settingsCategory,
                            onDismiss = { 
                                showSettings = false
                                settingsCategory = null
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Search overlay
                    AnimatedVisibility(
                        visible = showSearch,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeOut(animationSpec = tween(300))
                    ) {
                        SearchScreen(
                            onDismiss = { showSearch = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Vault overlay
                    AnimatedVisibility(
                        visible = showVault,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeOut(animationSpec = tween(300))
                    ) {
                        VaultScreen(
                            onDismiss = { showVault = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // OrbSearch overlay
                    AnimatedVisibility(
                        visible = showOrbSearch,
                        enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                            initialScale = 0.9f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ),
                        exit = fadeOut(animationSpec = tween(150)) + scaleOut(
                            targetScale = 0.9f,
                            animationSpec = tween(150)
                        )
                    ) {
                        OrbSearchSurface(
                            onDismiss = { showOrbSearch = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        authManager.setActivity(this)
    }

    override fun onPause() {
        super.onPause()
        authManager.clearActivity()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.categories?.contains(Intent.CATEGORY_HOME) == true) {
            homePresses.tryEmit(Unit)
        }
    }

    private fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                roleRequestLauncher.launch(intent)
            }
        }
    }

    private fun registerScreenOffReceiver() {
        if (screenOffReceiver != null) return
        screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    lifecycleScope.launch {
                        settingsRepository.settings.firstOrNull()?.let { settings ->
                            if (settings.appLockScreenOff) {
                                authManager.lastAuthenticatedTime = 0L
                            }
                        }
                    }
                }
            }
        }
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    private fun unregisterScreenOffReceiver() {
        screenOffReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {}
        }
        screenOffReceiver = null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterScreenOffReceiver()
    }
}
