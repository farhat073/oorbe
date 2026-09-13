package com.oorbitt.launcher.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The Orb — a floating, always-visible circular button on the home screen.
 * Tapping opens the OrbSpace Search Surface.
 *
 * Features:
 * - Radial gradient glow using Material You primary color
 * - Draggable with edge-snapping on release
 * - Pulse animation when [isPulsing] is true (clipboard content detected)
 */
@Composable
fun OrbButton(
    onClick: () -> Unit,
    isPulsing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val orbSizeDp = 56.dp
    val orbSizePx = with(density) { orbSizeDp.toPx() }
    val glowRadiusPx = with(density) { 40.dp.toPx() }

    // Material You dynamic color
    val primaryColor = MaterialTheme.colorScheme.primary
    val glowColor = primaryColor.copy(alpha = 0.45f)
    val orbGradientStart = primaryColor.copy(alpha = 0.95f)
    val orbGradientEnd = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)

    // Container size tracking
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Position state — start bottom-center
    var offsetX by remember { mutableFloatStateOf(-1f) } // -1 signals "not initialized"
    var offsetY by remember { mutableFloatStateOf(-1f) }
    var isDragging by remember { mutableStateOf(false) }

    // Initialize to bottom-center once container size is known
    LaunchedEffect(containerSize) {
        if (containerSize.width > 0 && offsetX < 0f) {
            offsetX = (containerSize.width - orbSizePx) / 2f
            offsetY = containerSize.height - orbSizePx - with(density) { 28.dp.toPx() }
        }
    }

    // Pulse animation (scale oscillation 1.0 → 1.12 → 1.0)
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Glow breathing animation (always active, subtle)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Drag scale
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dragScale"
    )

    // Edge-snap animation
    val animatedX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "snapX"
    )
    val animatedY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "snapY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        if (containerSize.width > 0 && offsetX >= 0f) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (if (isDragging) offsetX else animatedX).roundToInt(),
                            (if (isDragging) offsetY else animatedY).roundToInt()
                        )
                    }
                    .size(orbSizeDp)
                    .scale(pulseScale * dragScale)
                    // Glow layer
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = glowAlpha),
                                    glowColor.copy(alpha = glowAlpha * 0.4f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2, size.height / 2),
                                radius = size.width / 2 + glowRadiusPx
                            )
                        )
                    }
                    .shadow(
                        elevation = if (isDragging) 16.dp else 8.dp,
                        shape = CircleShape,
                        ambientColor = primaryColor.copy(alpha = 0.3f),
                        spotColor = primaryColor.copy(alpha = 0.4f)
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(orbGradientStart, orbGradientEnd)
                        )
                    )
                    // Drag gesture
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                // Snap to nearest horizontal edge
                                val midX = containerSize.width / 2f
                                offsetX = if (offsetX + orbSizePx / 2 < midX) {
                                    with(density) { 12.dp.toPx() } // Left edge
                                } else {
                                    containerSize.width - orbSizePx - with(density) { 12.dp.toPx() } // Right edge
                                }
                                // Clamp vertical
                                offsetY = offsetY.coerceIn(
                                    with(density) { 48.dp.toPx() },
                                    containerSize.height - orbSizePx - with(density) { 16.dp.toPx() }
                                )
                            },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX = (offsetX + dragAmount.x).coerceIn(
                                    0f,
                                    (containerSize.width - orbSizePx).coerceAtLeast(0f)
                                )
                                offsetY = (offsetY + dragAmount.y).coerceIn(
                                    0f,
                                    (containerSize.height - orbSizePx).coerceAtLeast(0f)
                                )
                            }
                        )
                    }
                    // Tap gesture
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Inner orb icon — a stylized "O" ring
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .drawBehind {
                            // Draw a ring
                            drawCircle(
                                color = Color.White.copy(alpha = 0.9f),
                                radius = size.width / 2,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = size.width * 0.18f
                                )
                            )
                            // Inner dot
                            drawCircle(
                                color = Color.White.copy(alpha = 0.7f),
                                radius = size.width * 0.15f
                            )
                        }
                )
            }
        }
    }
}
