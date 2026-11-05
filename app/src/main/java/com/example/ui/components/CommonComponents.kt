package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderColor: Color = FlareDarkCardBorder,
    backgroundColor: Color = FlareDarkSurface.copy(alpha = 0.85f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun TypingBubble(
    senderName: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")

    @Composable
    fun dotAnim(delayMillis: Int): Float {
        val anim by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -6f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    0f at 0
                    -6f at 300 + delayMillis
                    0f at 600 + delayMillis
                    0f at 1000
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "dot_$delayMillis"
        )
        return anim
    }

    val offset1 = dotAnim(0)
    val offset2 = dotAnim(150)
    val offset3 = dotAnim(300)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FlareDarkSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$senderName sedang mengetik",
            fontSize = 12.sp,
            color = FlareTextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .offset(y = offset1.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(FlareOrange)
            )
            Box(
                modifier = Modifier
                    .offset(y = offset2.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(FlareOrange)
            )
            Box(
                modifier = Modifier
                    .offset(y = offset3.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(FlareOrange)
            )
        }
    }
}

@Composable
fun E2EEBadge(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F231D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "E2EE",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "E2EE Aktif",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF10B981)
            )
        }
    }
}
