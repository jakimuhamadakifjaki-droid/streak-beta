package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.getStreakColor
import com.example.ui.theme.getStreakGradient
import kotlin.math.sin

@Composable
fun FlameBadge(
    streakCount: Int,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showNumber: Boolean = true,
    animate: Boolean = true
) {
    val streakColor = getStreakColor(streakCount)
    val streakGradient = getStreakGradient(streakCount)

    val infiniteTransition = rememberInfiniteTransition(label = "flame_anim")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val currentScale = if (animate && streakCount > 0) scaleAnim else 1f

    Row(
        modifier = modifier
            .scale(currentScale)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141C2E).copy(alpha = 0.85f))
            .border(
                width = 1.5.dp,
                brush = streakGradient,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = "Flame Streak",
            tint = streakColor,
            modifier = Modifier.size(size * 0.7f)
        )
        if (showNumber) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$streakCount",
                color = streakColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.45f).sp
            )
        }
    }
}

@Composable
fun AnimatedHeroFlame(
    streakCount: Int,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_flame")
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flicker"
    )

    val streakColor = getStreakColor(streakCount)
    val streakGradient = getStreakGradient(streakCount)

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 20.dp,
                shape = CircleShape,
                ambientColor = streakColor,
                spotColor = streakColor
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val phase = flicker * 2 * Math.PI.toFloat()

            // Outer flame glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(streakColor.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(w / 2, h / 2),
                    radius = w * 0.55f
                )
            )

            // Dynamic flame path
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.08f + sin(phase) * 6f)
                cubicTo(
                    w * 0.75f, h * 0.35f,
                    w * 0.95f, h * 0.65f,
                    w * 0.75f, h * 0.88f
                )
                cubicTo(
                    w * 0.60f, h * 0.98f,
                    w * 0.40f, h * 0.98f,
                    w * 0.25f, h * 0.88f
                )
                cubicTo(
                    w * 0.05f, h * 0.65f,
                    w * 0.25f, h * 0.35f,
                    w * 0.5f, h * 0.08f + sin(phase) * 6f
                )
                close()
            }
            drawPath(path = path, brush = streakGradient, style = Fill)

            // Inner core flame
            val innerPath = Path().apply {
                moveTo(w * 0.5f, h * 0.4f)
                cubicTo(
                    w * 0.65f, h * 0.55f,
                    w * 0.75f, h * 0.75f,
                    w * 0.6f, h * 0.88f
                )
                cubicTo(
                    w * 0.55f, h * 0.92f,
                    w * 0.45f, h * 0.92f,
                    w * 0.4f, h * 0.88f
                )
                cubicTo(
                    w * 0.25f, h * 0.75f,
                    w * 0.35f, h * 0.55f,
                    w * 0.5f, h * 0.4f
                )
                close()
            }
            drawPath(
                path = innerPath,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFFF7ED), Color(0xFFFFD700))
                ),
                style = Fill
            )
        }

        // Prominent streak number in center
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (streakCount > 0) "$streakCount" else "0",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "HARI",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}
