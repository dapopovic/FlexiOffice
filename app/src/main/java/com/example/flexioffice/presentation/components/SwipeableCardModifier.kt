package com.example.flexioffice.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Custom Modifier für horizontales Swipe-Verhalten mit smooth Animation und Callback.
 *
 * @param onSwipeLeft Callback bei erfolgreichem Swipe nach links
 * @param onSwipeRight Callback bei erfolgreichem Swipe nach rechts
 * @param isEnabled Ob Swipen erlaubt ist
 * @param swipeThresholdFraction Anteil der Bildschirmbreite, der für einen Swipe erreicht werden muss (z.B. 0.5f = Hälfte)
 * @param onOffsetChange Optionaler Callback für Offset-Änderungen (z.B. für Farbe)
 * @param animationDurationMs Dauer der Rück-Animation in Millisekunden
 */
fun Modifier.swipeableCard(
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    isEnabled: Boolean = true,
    swipeThresholdFraction: Float = 0.5f,
    onOffsetChange: ((Float, Float) -> Unit)? = null,
    animationDurationMs: Int = 200,
): Modifier = composed {
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val swipeThreshold = screenWidthPx * swipeThresholdFraction
    
    val offsetX = remember { Animatable(0f) }
    var isProcessing by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Trigger offset change callback whenever offset changes
    LaunchedEffect(offsetX.value) {
        onOffsetChange?.invoke(offsetX.value, swipeThreshold)
    }

    this
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .pointerInput(isEnabled, isProcessing) {
            if (!isEnabled || isProcessing) return@pointerInput
            detectDragGestures(
                onDragStart = {
                    isDragging = true
                    scope.launch {
                        offsetX.stop() // Stop any ongoing animation
                    }
                },
                onDragEnd = {
                    isDragging = false
                    scope.launch {
                        if (abs(offsetX.value) >= swipeThreshold) {
                            isProcessing = true
                            
                            // Animate to completion first for visual feedback
                            val targetX = if (offsetX.value > 0) screenWidthPx else -screenWidthPx
                            offsetX.animateTo(
                                targetValue = targetX,
                                animationSpec = tween(durationMillis = 150)
                            )
                            
                            // Trigger callback
                            if (offsetX.value > 0) onSwipeRight() else onSwipeLeft()
                            
                            // Reset and delay to prevent rapid successive swipes
                            offsetX.snapTo(0f)
                            kotlinx.coroutines.delay(300)
                            isProcessing = false
                        } else {
                            // Smooth spring-back animation
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = animationDurationMs,
                                    easing = EaseOutQuart
                                )
                            )
                        }
                    }
                },
                onDragCancel = {
                    isDragging = false
                    scope.launch {
                        offsetX.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = animationDurationMs)
                        )
                    }
                }
            ) { _, dragAmount ->
                if (!isProcessing && isDragging) {
                    scope.launch {
                        val newValue = (offsetX.value + dragAmount.x)
                            .coerceIn(-screenWidthPx * 0.8f, screenWidthPx * 0.8f) // Slight resistance at edges
                        offsetX.snapTo(newValue)
                    }
                }
            }
        }
}
