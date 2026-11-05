package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OwnerGoldCheckmark(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    showGleam: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "owner_badge")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slow_spin"
    )

    val gleamAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gleam"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        // Rotating sparkle ray background
        if (showGleam) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotation)
            ) {
                val cx = this.size.width / 2
                val cy = this.size.height / 2
                val radius = this.size.width * 0.48f

                // Draw 8-star shimmer gleam rays
                for (i in 0 until 8) {
                    val angle = (i * Math.PI / 4).toFloat()
                    val x = cx + cos(angle) * radius
                    val y = cy + sin(angle) * radius
                    drawLine(
                        color = Color(0xFFFFE600).copy(alpha = gleamAlpha * 0.5f),
                        start = Offset(cx, cy),
                        end = Offset(x, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }

        // Golden Badge base
        Box(
            modifier = Modifier
                .size(size * 0.85f)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFFFFD700),
                    spotColor = Color(0xFFFFB703)
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFFFF9C4),
                            Color(0xFFFFD700),
                            Color(0xFFFF9E00)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Verified Owner Badge",
                tint = Color(0xFF3E2723),
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}
