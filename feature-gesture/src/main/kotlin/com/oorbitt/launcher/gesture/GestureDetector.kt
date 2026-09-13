package com.oorbitt.launcher.gesture

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs
import kotlin.math.sqrt

private fun PointerEvent.calculateCentroidSize(): Float {
    if (changes.size < 2) return 0f
    var sumX = 0f
    var sumY = 0f
    changes.forEach {
        sumX += it.position.x
        sumY += it.position.y
    }
    val centroidX = sumX / changes.size
    val centroidY = sumY / changes.size

    var sumDistance = 0f
    changes.forEach {
        val dx = it.position.x - centroidX
        val dy = it.position.y - centroidY
        sumDistance += sqrt(dx * dx + dy * dy)
    }
    return sumDistance / changes.size
}

@Composable
fun Modifier.detectLauncherGestures(
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onDoubleTap: () -> Unit,
    onPinchIn: () -> Unit,
    onPinchOut: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    isSystemDragging: () -> Boolean = { false }
): Modifier {
    val currentIsSystemDragging by rememberUpdatedState(isSystemDragging)
    return this
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { onDoubleTap() },
                onLongPress = { onLongPress?.invoke() }
            )
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    var pinchActive = false
                    var initialCentroidSize = 0f
                    var gestureInterrupted = false

                    // Wait for the first down press using PointerEventPass.Initial
                    awaitPointerEvent(PointerEventPass.Initial)
                    var dragAccumulatorY = 0f

                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val changes = event.changes

                        if (currentIsSystemDragging()) {
                            gestureInterrupted = true
                        }

                        if (changes.any { it.isConsumed }) {
                            gestureInterrupted = true
                        }

                        if (!gestureInterrupted) {
                            val count = changes.size
                            if (count >= 2) {
                                // Pinch detection
                                val centroidSize = event.calculateCentroidSize()
                                if (!pinchActive) {
                                    initialCentroidSize = centroidSize
                                    pinchActive = true
                                } else {
                                    val ratio = centroidSize / if (initialCentroidSize <= 0f) 1f else initialCentroidSize
                                    if (ratio < 0.75f) {
                                        onPinchIn()
                                        gestureInterrupted = true
                                        changes.forEach { it.consume() }
                                    } else if (ratio > 1.25f) {
                                        onPinchOut()
                                        gestureInterrupted = true
                                        changes.forEach { it.consume() }
                                    }
                                }
                            } else if (count == 1 && !pinchActive) {
                                // Vertical swipes detection
                                val change = changes.first()
                                if (change.positionChanged()) {
                                    val diffY = change.position.y - change.previousPosition.y
                                    dragAccumulatorY += diffY
                                    if (abs(dragAccumulatorY) > 80f) {
                                        if (dragAccumulatorY < 0) {
                                            onSwipeUp()
                                        } else {
                                            onSwipeDown()
                                        }
                                        gestureInterrupted = true
                                        change.consume()
                                    }
                                }
                            }
                        }
                    } while (changes.any { it.pressed })
                }
            }
        }
}
