package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationItem
import com.example.data.repository.FlareRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    repository: FlareRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val coroutineScope = rememberCoroutineScope()
    val notifications by repository.getNotificationsFlow().collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlareDarkBackground)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = FlareDarkSurface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = FlareTextPrimary
                    )
                }

                Text(
                    text = "Pusat Notifikasi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlareTextPrimary,
                    modifier = Modifier.weight(1f)
                )

                if (notifications.isNotEmpty()) {
                    IconButton(onClick = {
                        coroutineScope.launch { repository.clearAllNotifications() }
                    }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear all",
                            tint = FlareTextSecondary
                        )
                    }
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = FlareTextTertiary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum ada notifikasi baru",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlareTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications) { notif ->
                    val (icon, color) = when (notif.type) {
                        "MILESTONE" -> Icons.Default.EmojiEvents to FlareGold
                        "STREAK_EXPIRING" -> Icons.Default.HourglassBottom to FlareCrimson
                        "FRIEND_REQUEST" -> Icons.Default.PersonAdd to FlarePurple
                        "FRIEND_ACCEPTED" -> Icons.Default.CheckCircle to FlareGreen
                        else -> Icons.Default.LocalFireDepartment to FlareOrange
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = FlareDarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, FlareDarkCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FlareTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = notif.body,
                                    fontSize = 12.sp,
                                    color = FlareTextSecondary,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(notif.timestamp)),
                                    fontSize = 10.sp,
                                    color = FlareTextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
