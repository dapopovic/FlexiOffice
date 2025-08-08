package com.example.flexioffice.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flexioffice.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Creates a shimmer effect modifier */
fun Modifier.customShimmerEffect(
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
                    start =
                        Offset(
                            x = translateAnim.value - 200f,
                            y = translateAnim.value - 200f,
                        ),
                    end = Offset(x = translateAnim.value, y = translateAnim.value),
                ),
            shape = RoundedCornerShape(12.dp),
        )
    }

/** Creates a pulsing glow effect */
fun Modifier.customPulseGlow(
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

@Composable
fun InAppNotificationBanner(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    type: String? = null,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAction: (() -> Unit)? = null,
) {
    val hapticFeedback = LocalHapticFeedback.current

    // Animation states
    var isAnimatingIn by remember { mutableStateOf(false) }
    var isAnimatingOut by remember { mutableStateOf(false) }

    // Trigger haptic feedback when notification appears
    LaunchedEffect(isVisible) {
        if (isVisible) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Auto dismiss timer
    LaunchedEffect(isVisible) {
        if (isVisible) {
            isAnimatingIn = true
            delay(100) // Small delay for entrance animation
            isAnimatingIn = false

            // Auto dismiss after 5 seconds
            delay(5000)
            isAnimatingOut = true
            delay(300) // Animation duration
            onDismiss()
        }
    }

    // Reset animation states when visibility changes
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            isAnimatingIn = false
            isAnimatingOut = false
        }
    }

    // Animation values
    val slideOffset by
        animateFloatAsState(
            targetValue = if (isVisible && !isAnimatingOut) 0f else -100f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "slideOffset",
        )

    val alpha by
        animateFloatAsState(
            targetValue = if (isVisible && !isAnimatingOut) 1f else 0f,
            animationSpec =
                tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing,
                ),
            label = "alpha",
        )

    val scale by
        animateFloatAsState(
            targetValue = if (isVisible && !isAnimatingOut) 1f else 0.8f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            label = "scale",
        )

    // Icon bounce animation
    val iconBounce by
        animateFloatAsState(
            targetValue = if (isAnimatingIn) 1.2f else 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessHigh,
                ),
            label = "iconBounce",
        )

    AnimatedVisibility(
        visible = isVisible,
        enter =
            slideInVertically(
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                initialOffsetY = { -it },
            ) +
                fadeIn(
                    animationSpec = tween(300),
                ),
        exit =
            slideOutVertically(
                animationSpec = tween(300),
                targetOffsetY = { -it },
            ) +
                fadeOut(
                    animationSpec = tween(300),
                ),
    ) {
        val backgroundColor =
            when (type) {
                "booking_status_update" -> MaterialTheme.colorScheme.primaryContainer
                "new_booking_request" -> MaterialTheme.colorScheme.secondaryContainer
                "team_invitation" -> MaterialTheme.colorScheme.tertiaryContainer
                "team_invitation_response" -> MaterialTheme.colorScheme.primaryContainer
                "team_invitation_cancelled" -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

        val icon =
            when (type) {
                "booking_status_update" -> Icons.Default.CheckCircle
                "new_booking_request" -> Icons.Default.Notifications
                "team_invitation" -> Icons.Default.Notifications
                "team_invitation_response" -> Icons.Default.CheckCircle
                "team_invitation_cancelled" -> Icons.Default.Notifications
                else -> Icons.Default.Notifications
            }
        Card(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        translationY = slideOffset
                    }.then(
                        when (type) {
                            "new_booking_request" -> {
                                // Add shimmer effect for new booking requests
                                Modifier.customShimmerEffect(
                                    isEnabled = true,
                                    colors =
                                        listOf(
                                            Color.White.copy(
                                                alpha = 0.0f,
                                            ),
                                            Color.White.copy(
                                                alpha = 0.4f,
                                            ),
                                            Color.White.copy(
                                                alpha = 0.0f,
                                            ),
                                        ),
                                )
                            }
                            "team_invitation" -> {
                                // Subtle shimmer to draw attention
                                Modifier.customShimmerEffect(isEnabled = true)
                            }
                            "team_invitation_cancelled" -> {
                                // Pulse glow for cancelled/urgent-like
                                Modifier.customPulseGlow(
                                    isEnabled = true,
                                    glowColor = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                                    pulseSpeed = 900,
                                )
                            }
                            "urgent" -> {
                                // Add pulse glow for urgent notifications
                                Modifier.customPulseGlow(
                                    isEnabled = true,
                                    glowColor =
                                        MaterialTheme.colorScheme.error
                                            .copy(alpha = 0.3f),
                                    pulseSpeed = 800,
                                )
                            }
                            else -> {
                                Modifier
                            }
                        },
                    ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).scale(iconBounce),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (onAction != null) {
                    // Animated action button
                    val buttonScale by
                        animateFloatAsState(
                            targetValue = if (isAnimatingIn) 1.1f else 1f,
                            animationSpec =
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            label = "buttonScale",
                        )

                    TextButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.TextHandleMove,
                            )
                            isAnimatingOut = true
                            onAction()
                        },
                        modifier = Modifier.scale(buttonScale),
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) { Text(stringResource(R.string.in_app_notification_action_button)) }
                }

                // Animated close button
                val closeButtonRotation by
                    animateFloatAsState(
                        targetValue = if (isAnimatingOut) 180f else 0f,
                        animationSpec = tween(300),
                        label = "closeButtonRotation",
                    )

                IconButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isAnimatingOut = true
                        // Small delay to show animation before dismissing
                        MainScope().launch {
                            delay(150)
                            onDismiss()
                        }
                    },
                    modifier =
                        Modifier.size(24.dp).graphicsLayer {
                            rotationZ = closeButtonRotation
                        },
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.in_app_notification_close),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
