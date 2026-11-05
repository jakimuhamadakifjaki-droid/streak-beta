package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AchievementDefinition
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AchievementBadgeItem(
    achievement: AchievementDefinition,
    isUnlocked: Boolean,
    unlockedAt: Long? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tierGradient = when (achievement.tier) {
        "Bronze" -> AchievementBadges.BronzeGradient
        "Silver" -> AchievementBadges.SilverGradient
        "Gold" -> AchievementBadges.GoldGradient
        "Diamond" -> AchievementBadges.DiamondGradient
        "Cosmic" -> AchievementBadges.CosmicGradient
        else -> AchievementBadges.GoldGradient
    }

    val tierColor = when (achievement.tier) {
        "Bronze" -> Color(0xFFCD7F32)
        "Silver" -> Color(0xFFCBD5E1)
        "Gold" -> Color(0xFFFFD700)
        "Diamond" -> Color(0xFF00F2FE)
        "Cosmic" -> Color(0xFFFF007A)
        else -> FlareOrange
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isUnlocked) FlareDarkSurface else FlareDarkSurfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isUnlocked) tierGradient else Brush.linearGradient(listOf(FlareDarkCardBorder, FlareDarkCardBorder))
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Distinct Shield Medal Emblem (Distinct from Streak Flames)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isUnlocked) tierGradient else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                    )
                    .border(
                        1.dp,
                        if (isUnlocked) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Text(
                        text = achievement.iconEmoji,
                        fontSize = 24.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Terkunci",
                        tint = FlareTextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = achievement.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) FlareTextPrimary else FlareTextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Tier Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tierColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, tierColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = achievement.tier,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tierColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = achievement.description,
                    fontSize = 11.sp,
                    color = FlareTextSecondary,
                    lineHeight = 15.sp,
                    maxLines = 2
                )

                if (isUnlocked && unlockedAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Terbuka: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(unlockedAt))}",
                        fontSize = 10.sp,
                        color = tierColor
                    )
                }
            }

            if (isUnlocked) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Unlocked",
                    tint = tierColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
