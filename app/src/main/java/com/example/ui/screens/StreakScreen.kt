package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StreakMilestones
import com.example.data.repository.FlareRepository
import com.example.data.repository.StreakWithUser
import com.example.ui.components.AnimatedHeroFlame
import com.example.ui.components.FlameBadge
import com.example.ui.components.FlareAvatar
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun StreakScreen(
    repository: FlareRepository,
    onNavigateToChat: (friendId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val streaksWithUsers by repository.getStreaksFlow().collectAsState(initial = emptyList())
    var previewLevel by remember { mutableIntStateOf(14) }

    val maxStreak = streaksWithUsers.maxOfOrNull { it.streak.currentStreak } ?: 0

    val previewPresets = listOf(1, 7, 14, 30, 60, 100, 365, 500, 1000)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FlareDarkBackground)
            .padding(bottom = 90.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = FlareOrange,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "FLAME STREAKS",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = FlareOrange,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Api tanpa batas dengan spektrum gradasi multi-warna dinamis",
                        fontSize = 12.sp,
                        color = FlareTextSecondary
                    )
                }
            }
        }

        // Hero Interactive Flame Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                borderColor = getStreakColor(previewLevel).copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SPEKTRUM PANAS MULTI-WARNA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlareTextSecondary,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedHeroFlame(
                        streakCount = previewLevel,
                        size = 140.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val milestone = StreakMilestones.getMilestoneForDays(previewLevel)
                    AnimatedContent(
                        targetState = milestone,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "milestone_title"
                    ) { ms ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = ms?.title ?: "Flame Active",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = getStreakColor(previewLevel)
                            )
                            Text(
                                text = ms?.subtitle ?: "Sentuhan api persahabatan",
                                fontSize = 12.sp,
                                color = FlareTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preset Multi-color Spectrum Chips
                    Text(
                        text = "Pilih Tingkat Spektrum Panas:",
                        fontSize = 11.sp,
                        color = FlareTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        previewPresets.forEach { lvl ->
                            val isSelected = previewLevel == lvl
                            Surface(
                                onClick = { previewLevel = lvl },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) getStreakColor(lvl) else FlareDarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.White else Color.Transparent
                                )
                            ) {
                                Text(
                                    text = if (lvl >= 1000) "1000d" else "${lvl}d",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else FlareTextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Streaks Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Api Persahabatan Aktif",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlareTextPrimary
                )
                Text(
                    text = "${streaksWithUsers.size} Teman",
                    fontSize = 12.sp,
                    color = FlareTextSecondary
                )
            }
        }

        if (streaksWithUsers.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = FlareDarkSurface
                ) {
                    Text(
                        text = "Belum ada streak yang menyala. Kirim ajakan 'Mau bikin api gak?' ke teman untuk menyalakan api pertamamu!",
                        color = FlareTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(streaksWithUsers) { item ->
                Surface(
                    onClick = { onNavigateToChat(item.friend.id) },
                    shape = RoundedCornerShape(20.dp),
                    color = FlareDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        getStreakColor(item.streak.currentStreak).copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlareAvatar(
                            name = item.friend.displayName,
                            avatarIndex = item.friend.avatarIndex,
                            customPhotoUri = item.friend.customPhotoUri,
                            size = 52.dp,
                            streakCount = item.streak.currentStreak,
                            isOwner = item.friend.isVerifiedOwner
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.friend.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlareTextPrimary
                            )
                            Text(
                                text = item.milestone?.title ?: "Streak Aktif",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = getStreakColor(item.streak.currentStreak)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = FlareTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sisa waktu hari ini: ${item.hoursRemainingToday} jam",
                                    fontSize = 11.sp,
                                    color = FlareTextSecondary
                                )
                            }
                        }

                        FlameBadge(
                            streakCount = item.streak.currentStreak,
                            size = 32.dp
                        )
                    }
                }
            }
        }

        // Streak Milestones Roadmap (Unlimited Days)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tingkatan & Milestone Api Tanpa Batas",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = FlareTextPrimary
            )
        }

        items(StreakMilestones.list) { ms ->
            val isUnlocked = maxStreak >= ms.days
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isUnlocked) FlareDarkSurface else FlareDarkSurfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUnlocked) getStreakColor(ms.days).copy(alpha = 0.5f) else FlareDarkCardBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isUnlocked) getStreakColor(ms.days).copy(alpha = 0.2f) else FlareDarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = ms.iconEmoji, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ms.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) FlareTextPrimary else FlareTextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${ms.days} Hari",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = getStreakColor(ms.days)
                            )
                        }
                        Text(
                            text = ms.subtitle,
                            fontSize = 12.sp,
                            color = FlareTextSecondary
                        )
                    }

                    if (isUnlocked) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Unlocked",
                            tint = getStreakColor(ms.days),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = FlareTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
