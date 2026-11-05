package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.User
import com.example.data.repository.FlareRepository
import com.example.data.repository.StreakWithUser
import com.example.data.repository.UserWithFriendship
import com.example.ui.components.FlameBadge
import com.example.ui.components.FlareAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.OwnerGoldCheckmark
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    currentUser: User,
    repository: FlareRepository,
    onNavigateToChat: (friendId: Long) -> Unit,
    onNavigateToStreak: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onOpenStatus: (statusItem: com.example.data.model.StatusWithUser) -> Unit,
    unreadNotifications: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val friendsWithStatus by repository.getFriendsForCurrentUser().collectAsState(initial = emptyList())
    val streaksWithUsers by repository.getStreaksFlow().collectAsState(initial = emptyList())
    val announcements by repository.getAnnouncementsFlow().collectAsState(initial = emptyList())
    val allStatusesWithUsers by repository.getActiveStatusesWithUsersFlow().collectAsState(initial = emptyList())

    var showCreateStatusSheet by remember { mutableStateOf(false) }

    val activeFlames = streaksWithUsers.filter { it.streak.currentStreak > 0 }
    val acceptedFriends = friendsWithStatus.filter { it.friendshipStatus == "ACCEPTED" }
    val myStatus = allStatusesWithUsers.firstOrNull { it.status.userId == currentUser.id }
    val otherStatuses = allStatusesWithUsers.filter { it.status.userId != currentUser.id }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlareDarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp) // Leave space for bottom nav
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_flare_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "FLARE",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = FlareOrange,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Keep The Flame Alive",
                        fontSize = 11.sp,
                        color = FlareTextSecondary
                    )
                }
            }

            // Notification Bell with Badge
            IconButton(
                onClick = onNavigateToNotifications,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(FlareDarkSurfaceVariant)
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotifications > 0) {
                            Badge(containerColor = FlareCrimson) {
                                Text("$unreadNotifications", fontSize = 10.sp)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = FlareTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // User Greeting & Quick Profile Summary Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            cornerRadius = 24.dp,
            borderColor = FlareDarkCardBorder
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlareAvatar(
                    name = currentUser.displayName,
                    avatarIndex = currentUser.avatarIndex,
                    customPhotoUri = currentUser.customPhotoUri,
                    size = 54.dp,
                    isOwner = currentUser.isVerifiedOwner
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentUser.displayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlareTextPrimary
                        )
                        if (currentUser.isVerifiedOwner) {
                            Spacer(modifier = Modifier.width(6.dp))
                            OwnerGoldCheckmark(size = 18.dp)
                        }
                    }
                    Text(
                        text = "@${currentUser.username}",
                        fontSize = 12.sp,
                        color = FlareTextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 6-Digit ID Box with Copy Button
                    Surface(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("FLARE ID", currentUser.sixDigitId))
                            Toast.makeText(context, "ID ${currentUser.sixDigitId} disalin!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = FlareDarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, FlareOrange.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ID: ${currentUser.sixDigitId}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FlareOrangeLight
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy ID",
                                tint = FlareOrangeLight,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Status Stories Section (Upload Status Teks, Foto, Video & View Status)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CircleNotifications,
                    contentDescription = null,
                    tint = FlareOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Status FLARE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlareTextPrimary
                )
            }

            Surface(
                onClick = { showCreateStatusSheet = true },
                shape = RoundedCornerShape(12.dp),
                color = FlareOrange.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, FlareOrange.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = FlareOrange, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Buat Status", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FlareOrange)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // My Status Item (Create or View)
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            if (myStatus != null) {
                                onOpenStatus(myStatus)
                            } else {
                                showCreateStatusSheet = true
                            }
                        }
                        .padding(2.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.5.dp,
                                    brush = if (myStatus != null) Brush.linearGradient(listOf(FlareOrange, FlareCrimson)) else Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B))),
                                    shape = CircleShape
                                )
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FlareAvatar(
                                name = currentUser.displayName,
                                avatarIndex = currentUser.avatarIndex,
                                customPhotoUri = currentUser.customPhotoUri,
                                size = 56.dp,
                                isOnline = currentUser.isOnline,
                                isOwner = currentUser.isVerifiedOwner
                            )
                        }

                        // Add (+) badge if no status or camera badge
                        Surface(
                            onClick = { showCreateStatusSheet = true },
                            shape = CircleShape,
                            color = FlareOrange,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (myStatus != null) Icons.Default.Add else Icons.Default.Add,
                                    contentDescription = "Buat Status",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (myStatus != null) "Status Saya" else "Tambah Status",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (myStatus != null) FlareOrange else FlareTextSecondary
                    )
                }
            }

            // Other Users' Statuses
            items(otherStatuses) { stItem ->
                val ringGradient = if (stItem.isViewedByMe) {
                    Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF475569)))
                } else {
                    Brush.linearGradient(listOf(FlareOrange, FlarePurple, FlareCyan))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onOpenStatus(stItem) }
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(width = 2.5.dp, brush = ringGradient, shape = CircleShape)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FlareAvatar(
                            name = stItem.user.displayName,
                            avatarIndex = stItem.user.avatarIndex,
                            customPhotoUri = stItem.user.customPhotoUri,
                            size = 56.dp,
                            isOnline = stItem.user.isOnline,
                            isOwner = stItem.user.isVerifiedOwner
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stItem.user.displayName.take(8),
                        fontSize = 11.sp,
                        fontWeight = if (!stItem.isViewedByMe) FontWeight.Bold else FontWeight.Normal,
                        color = if (!stItem.isViewedByMe) Color.White else FlareTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active Flames (Story Reel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = FlareOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Active Flames",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlareTextPrimary
                )
            }

            Text(
                text = "Lihat Semua",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlareOrange,
                modifier = Modifier.clickable { onNavigateToStreak() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeFlames.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FlareDarkSurface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada api aktif. Kirim pesan ke teman untuk mulai streak! 🔥",
                    fontSize = 13.sp,
                    color = FlareTextSecondary
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(activeFlames) { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onNavigateToChat(item.friend.id) }
                            .padding(4.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomCenter) {
                            FlareAvatar(
                                name = item.friend.displayName,
                                avatarIndex = item.friend.avatarIndex,
                                customPhotoUri = item.friend.customPhotoUri,
                                size = 64.dp,
                                streakCount = item.streak.currentStreak,
                                isOnline = item.friend.isOnline,
                                isOwner = item.friend.isVerifiedOwner
                            )
                            FlameBadge(
                                streakCount = item.streak.currentStreak,
                                size = 20.dp,
                                modifier = Modifier.offset(y = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = item.friend.displayName.take(8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = FlareTextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Streak Countdown & Call to action
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable { onNavigateToStreak() },
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1528),
            border = androidx.compose.foundation.BorderStroke(1.dp, FlarePurple.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(FlarePurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = FlarePurple,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pengingat Streak Harian ⏳",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlareTextPrimary
                    )
                    Text(
                        text = "Jaga api tetap membara hari ini sebelum tengah malam!",
                        fontSize = 12.sp,
                        color = FlareTextSecondary
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = FlareTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Announcements Banner
        if (announcements.isNotEmpty()) {
            val announcement = announcements.first()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                color = FlareDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, FlareOrange.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = FlareOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = announcement.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlareOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = announcement.content,
                        fontSize = 12.sp,
                        color = FlareTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Featured Friends / Quick Start Chat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Teman Saya",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FlareTextPrimary
            )
            Text(
                text = "+ Tambah Teman (ID 6 Digit)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlareOrange,
                modifier = Modifier.clickable { onNavigateToFriends() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (acceptedFriends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FlareDarkSurface)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum memiliki teman. Cari teman dengan ID 6 Digit!",
                    color = FlareTextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                acceptedFriends.forEach { f ->
                    Surface(
                        onClick = { onNavigateToChat(f.user.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = FlareDarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, FlareDarkCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlareAvatar(
                                name = f.user.displayName,
                                avatarIndex = f.user.avatarIndex,
                                customPhotoUri = f.user.customPhotoUri,
                                size = 46.dp,
                                streakCount = f.streak?.currentStreak ?: 0,
                                isOnline = true,
                                isOwner = f.user.isVerifiedOwner
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = f.user.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FlareTextPrimary
                                    )
                                    if (f.user.isVerifiedOwner) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        OwnerGoldCheckmark(size = 15.dp)
                                    }
                                }
                                Text(
                                    text = "ID: ${f.user.sixDigitId} • @${f.user.username}",
                                    fontSize = 11.sp,
                                    color = FlareTextSecondary
                                )
                            }

                            if ((f.streak?.currentStreak ?: 0) > 0) {
                                FlameBadge(streakCount = f.streak?.currentStreak ?: 0, size = 26.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Chat",
                                    tint = FlareOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateStatusSheet) {
        CreateStatusBottomSheet(
            repository = repository,
            onDismiss = { showCreateStatusSheet = false },
            onStatusCreated = { showCreateStatusSheet = false }
        )
    }
}
