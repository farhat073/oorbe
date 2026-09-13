package com.oorbitt.launcher.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnboardingScreen(
                onFinish = {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            )
        }
    }
}

// Brand Colors (Elegant Emerald and Slate Blue)
val PrimaryPurple = Color(0xFF00B894)
val SecondaryBlue = Color(0xFF0984E3)
val TertiaryPink = Color(0xFF00D2D3)
val EmeraldGreen = Color(0xFF00B894)
val DarkBackground = Color(0xFF1E1E2E)
val CardBackground = Color(0xFF313244)
val TextPrimary = Color(0xFFCDD6F4)
val TextMuted = Color(0xFFBAC2DE)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 6 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Decorative glowing backgrounds
        DecorativeGlow()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Top Bar with Skip Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Oorbitt",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (pagerState.currentPage < 5) {
                    Text(
                        text = "Skip",
                        color = PrimaryPurple,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                // Jump directly to permission slide (5)
                                scope.launch {
                                    pagerState.animateScrollToPage(5)
                                }
                            }
                            .padding(8.dp)
                    )
                }
            }

            // Slides Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> SlideWelcome()
                    1 -> SlideSearchDemo()
                    2 -> SlideWellness()
                    3 -> SlideSecurity()
                    4 -> SlideMarketplace()
                    5 -> SlidePermissions(onFinish)
                }
            }

            // Bottom Navigation Area
            if (pagerState.currentPage < 5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pager Indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(6) { index ->
                            val active = pagerState.currentPage == index
                            val width by animateDpAsState(
                                targetValue = if (active) 24.dp else 8.dp,
                                label = "indicator_width"
                            )
                            val color by animateColorAsState(
                                targetValue = if (active) PrimaryPurple else CardBackground,
                                label = "indicator_color"
                            )
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }

                    // Next Button
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPurple,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Next",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DecorativeGlow() {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val translationX1 by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_x1"
    )
    val translationY1 by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_y1"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = translationX1.dp, y = translationY1.dp)
                .align(Alignment.TopStart)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryPurple.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = -translationX1.dp, y = -translationY1.dp)
                .align(Alignment.BottomEnd)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SecondaryBlue.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun SlideWelcome() {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )
    val glowSizeValue by infiniteTransition.animateFloat(
        initialValue = 140f,
        targetValue = 220f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_size"
    )
    val glowSize = glowSizeValue.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Breathing Orb representation (Orbit logo double ovals)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(280.dp)
                .fillMaxWidth()
        ) {
            // Glow backdrop
            Box(
                modifier = Modifier
                    .size(glowSize)
                    .blur(50.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00B894).copy(alpha = 0.35f),
                                Color(0xFF0984E3).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    // Oval dimensions
                    val ovalWidth = width * 0.90f
                    val ovalHeight = height * 0.36f
                    val strokeWidth = 8f
                    
                    // Draw first oval rotated at -30 degrees
                    rotate(degrees = -30f) {
                        drawOval(
                            color = Color(0xFF0984E3),
                            topLeft = androidx.compose.ui.geometry.Offset((width - ovalWidth) / 2, (height - ovalHeight) / 2),
                            size = Size(ovalWidth, ovalHeight),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                    
                    // Draw second oval rotated at 30 degrees
                    rotate(degrees = 30f) {
                        drawOval(
                            color = Color(0xFF00B894),
                            topLeft = androidx.compose.ui.geometry.Offset((width - ovalWidth) / 2, (height - ovalHeight) / 2),
                            size = Size(ovalWidth, ovalHeight),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                    
                    // Draw side arcs (Left & Right)
                    val arcSize = width * 0.8f
                    val arcTopLeft = androidx.compose.ui.geometry.Offset((width - arcSize) / 2, (height - arcSize) / 2)
                    
                    // Left arc
                    drawArc(
                        color = Color(0xFF00B894),
                        startAngle = 120f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Right arc
                    drawArc(
                        color = Color(0xFF00B894),
                        startAngle = 300f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Oorbitt",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "A beautiful, premium Android launcher centered around minimalism, productivity, and digital well-being.",
            color = TextMuted,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun SlideSearchDemo() {
    var searchInput by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            searchInput = ""
            step = 0
            delay(1200)
            
            // Simulating typing
            val text = "Wh"
            for (char in text) {
                searchInput += char
                delay(200)
            }
            step = 1
            delay(1500)

            val text2 = "atsApp"
            for (char in text2) {
                searchInput += char
                delay(150)
            }
            step = 2
            delay(2500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mock Phone Screen Search
        Box(
            modifier = Modifier
                .width(260.dp)
                .height(380.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(CardBackground)
                .padding(16.dp)
        ) {
            Column {
                // Mock search bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(DarkBackground)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (searchInput.isEmpty()) "Search anything..." else searchInput,
                        color = if (searchInput.isEmpty()) TextMuted.copy(alpha = 0.5f) else TextPrimary,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filtering list
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "search_results"
                ) { currentStep ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        when (currentStep) {
                            0 -> {
                                SearchResultRow(icon = Icons.Outlined.Info, title = "Oorbitt Settings")
                                SearchResultRow(icon = Icons.Outlined.Settings, title = "System Settings")
                                SearchResultRow(icon = Icons.Outlined.Star, title = "Favorites")
                            }
                            1 -> {
                                SearchResultRow(icon = Icons.Default.PlayArrow, title = "WhatsApp", highlight = true)
                                SearchResultRow(icon = Icons.Outlined.Person, title = "WhatsApp Contact: Wesley")
                                SearchResultRow(icon = Icons.Outlined.Search, title = "Web: What is OrbSpace?")
                            }
                            2 -> {
                                SearchResultRow(icon = Icons.Default.PlayArrow, title = "WhatsApp", highlight = true)
                                SearchResultRow(icon = Icons.Outlined.Person, title = "WhatsApp Chat: Work Group")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Universal Search",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Instantly launch apps, find contacts, run calculations, or search files. Parallel query workers return results in milliseconds.",
            color = TextMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun SearchResultRow(icon: ImageVector, title: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlight) PrimaryPurple.copy(alpha = 0.15f) else DarkBackground.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (highlight) PrimaryPurple else CardBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlight) DarkBackground else TextPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = if (highlight) PrimaryPurple else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun SlideWellness() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Bento wellness card
        Box(
            modifier = Modifier
                .width(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardBackground)
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "DIGITAL HYGIENE",
                    color = SecondaryBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Concentric Wellness Ring Mockup
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { 0.75f },
                        modifier = Modifier.size(100.dp),
                        color = SecondaryBlue,
                        strokeWidth = 10.dp,
                    )
                    CircularProgressIndicator(
                        progress = { 0.45f },
                        modifier = Modifier.size(76.dp),
                        color = TertiaryPink,
                        strokeWidth = 10.dp,
                    )
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Social Limit: 45m / 60m",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Total Swipes: 142",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Mindful Wellness",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Combats addictive scrolling. Tracks scroll cycles inside social media apps and reminds you when limit targets are reached.",
            color = TextMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun SlideSecurity() {
    var authenticated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            authenticated = false
            delay(1500)
            authenticated = true
            delay(2500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Vault layout animation mockup
        Box(
            modifier = Modifier
                .width(260.dp)
                .height(240.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Fingerprint/Shield icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (authenticated) EmeraldGreen.copy(alpha = 0.2f) else PrimaryPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (authenticated) Icons.Default.CheckCircle else Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = if (authenticated) EmeraldGreen else PrimaryPurple,
                        modifier = Modifier
                            .size(48.dp)
                            .scale(if (authenticated) 1.1f else 1.0f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (authenticated) "Access Granted" else "Biometric Security",
                    color = if (authenticated) EmeraldGreen else TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (authenticated) "Vault Unlocked" else "Scanning...",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "App Lock & Vault",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Biometric protection for focus-critical apps. Keep personal memos, photos, and apps hidden behind Android secure authentication.",
            color = TextMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun SlideMarketplace() {
    var previewShape by remember { mutableStateOf(IconShape.CIRCLE) }

    LaunchedEffect(Unit) {
        while (true) {
            previewShape = IconShape.CIRCLE
            delay(1500)
            previewShape = IconShape.SQUIRCLE
            delay(1500)
            previewShape = IconShape.ROUNDED_RECT
            delay(1500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Customization blob mockup
        Box(
            modifier = Modifier
                .width(260.dp)
                .height(240.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardBackground),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon shape rendering
                repeat(3) { index ->
                    val shape = when (previewShape) {
                        IconShape.CIRCLE -> CircleShape
                        IconShape.SQUIRCLE -> RoundedCornerShape(18.dp)
                        IconShape.ROUNDED_RECT -> RoundedCornerShape(8.dp)
                    }
                    val color = when (index) {
                        0 -> PrimaryPurple
                        1 -> SecondaryBlue
                        else -> TertiaryPink
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(shape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (index) {
                                0 -> Icons.Default.Home
                                1 -> Icons.Default.Mail
                                else -> Icons.Default.CameraAlt
                            },
                            contentDescription = null,
                            tint = DarkBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "OorbStyle Marketplace",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Backup, share, and discover beautiful setup layouts. Download premium themes created by the launcher community.",
            color = TextMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

enum class IconShape { CIRCLE, SQUIRCLE, ROUNDED_RECT }

@Composable
fun SlidePermissions(onFinish: () -> Unit) {
    val context = LocalContext.current
    var isAccessibilityEnabled by remember { mutableStateOf(false) }

    // Check accessibility status when returned to activity
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Accessibility Permission card
        Box(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardBackground)
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isAccessibilityEnabled) EmeraldGreen.copy(alpha = 0.2f) else SecondaryBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = if (isAccessibilityEnabled) EmeraldGreen else SecondaryBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Scroll Tracker Service",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enables background scroll tracking inside social media apps to help you monitor screen time. No personal data ever leaves the device.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAccessibilityEnabled) EmeraldGreen else SecondaryBlue,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isAccessibilityEnabled) "Service Active" else "Enable in Settings",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Ready to Begin?",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Enable Scroll Tracker for wellness statistics, or skip it. You can adjust all preferences anytime in settings.",
            color = TextMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryPurple,
                contentColor = DarkBackground
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 14.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                text = "Enter Launcher",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    for (service in enabledServices) {
        if (service.resolveInfo.serviceInfo.packageName == context.packageName) {
            return true
        }
    }
    return false
}
