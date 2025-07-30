package com.example.flexioffice.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Creates a shimmer effect modifier that can be applied to any composable
 */
fun Modifier.shimmerEffect(
    isEnabled: Boolean = true,
    colors: List<Color> =
        listOf(
            Color.White.copy(alpha = 0.0f),
            Color.White.copy(alpha = 0.3f),
            Color.White.copy(alpha = 0.0f),
        ),
): Modifier =
    composed {
        if (!isEnabled) return@composed this

        val transition = rememberInfiniteTransition(label = "shimmerTransition")
        val translateAnim =
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            tween(
                                durationMillis = 1200,
                                easing = FastOutSlowInEasing,
                            ),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "shimmerTranslation",
            )

        background(
            brush =
                Brush.linearGradient(
                    colors = colors,
                    start = Offset(x = translateAnim.value - 200f, y = translateAnim.value - 200f),
                    end = Offset(x = translateAnim.value, y = translateAnim.value),
                ),
            shape = RoundedCornerShape(12.dp),
        )
    }

/**
 * Creates a pulsing glow effect for important notifications
 */
fun Modifier.pulseGlow(
    isEnabled: Boolean = true,
    glowColor: Color = Color.White.copy(alpha = 0.4f),
    pulseSpeed: Int = 1000,
): Modifier =
    composed {
        if (!isEnabled) return@composed this

        val transition = rememberInfiniteTransition(label = "pulseTransition")
        val alpha =
            transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.8f,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            tween(
                                durationMillis = pulseSpeed,
                                easing = FastOutSlowInEasing,
                            ),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "pulseAlpha",
            )

        background(
            color = glowColor.copy(alpha = alpha.value),
            shape = RoundedCornerShape(12.dp),
        )
    }
