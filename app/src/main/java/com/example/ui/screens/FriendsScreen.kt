package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.repository.FlareRepository
import com.example.data.repository.UserWithFriendship
import com.example.ui.components.FlameBadge
import com.example.ui.components.FlareAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.OwnerGoldCheckmark
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun FriendsScreen(
    repository: FlareRepository,
    onNavigateToChat: (friendId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val friendsList by repository.getFriendsForCurrentUser().collectAsState(initial = emptyList())
    val currentUser by repository.currentUser.collectAsState()

    var searchIdQuery by remember { mutableStateOf("") }
    var searchedUserResult by remember { mutableStateOf<User?>(null) }
    var searchExecuted by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Semua") } // "Semua", "Online", "Streak", "Permintaan"
    var friendToRemove by remember { mutableStateOf<User?>(null) }

    val incomingRequests = friendsList.filter { it.friendshipStatus == "PENDING_INCOMING" }
    val acceptedFriends = friendsList.filter { it.friendshipStatus == "ACCEPTED" }
    val onlineFriends = acceptedFriends.filter { it.user.isOnline }

    val displayedFriends = when (selectedCategory) {
        "Online" -> onlineFriends
        "Streak" -> acceptedFriends.filter { (it.streak?.currentStreak ?: 0) > 0 }
        "Permintaan" -> incomingRequests
        else -> acceptedFriends
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlareDarkBackground)
            .padding(bottom = 90.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Teman & Relasi",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = FlareTextPrimary
            )
            Text(
                text = "Temukan teman menggunakan ID 6 Digit unik",
                fontSize = 12.sp,
                color = FlareTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6-Digit ID Search Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchIdQuery,
                    onValueChange = {
                        if (it.length <= 6) {
                            searchIdQuery = it
                            if (it.isEmpty()) {
                                searchExecuted = false
                                searchedUserResult = null
                            }
                        }
                    },
                    placeholder = { Text("Masukkan ID 6 Digit (contoh: 238491)") },
                    leadingIcon = {
                        Icon(Icons.Default.Tag, contentDescription = null, tint = FlareOrange)
                    },
                    trailingIcon = {
                        if (searchIdQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchIdQuery = ""
                                searchExecuted = false
                                searchedUserResult = null
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = FlareTextSecondary)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlareOrange,
                        unfocusedBorderColor = FlareDarkCardBorder,
                        focusedTextColor = FlareTextPrimary,
                        unfocusedTextColor = FlareTextPrimary,
                        focusedContainerColor = FlareDarkSurface,
                        unfocusedContainerColor = FlareDarkSurface
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            searchExecuted = true
                            searchedUserResult = repository.searchUserBySixDigitId(searchIdQuery)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlareOrange),
                    modifier = Modifier.height(54.dp),
                    enabled = searchIdQuery.length >= 3
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Cari", tint = Color.White)
                }
            }
        }

        // Search Result Card
        if (searchExecuted) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                if (searchedUserResult != null) {
                    val targetUser = searchedUserResult!!
                    val isAlreadyFriend = friendsList.any { it.user.id == targetUser.id && it.friendshipStatus == "ACCEPTED" }
                    val isPending = friendsList.any { it.user.id == targetUser.id && it.friendshipStatus.startsWith("PENDING") }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = FlareOrange.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlareAvatar(
                                name = targetUser.displayName,
                                avatarIndex = targetUser.avatarIndex,
                                customPhotoUri = targetUser.customPhotoUri,
                                size = 52.dp,
                                isOwner = targetUser.isVerifiedOwner
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = targetUser.displayName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FlareTextPrimary
                                    )
                                    if (targetUser.isVerifiedOwner) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        OwnerGoldCheckmark(size = 15.dp)
                                    }
                                }
                                Text(
                                    text = "@${targetUser.username} • ID: ${targetUser.sixDigitId}",
                                    fontSize = 12.sp,
                                    color = FlareOrangeLight
                                )
                                Text(
                                    text = targetUser.bio.ifBlank { "Pengguna FLARE" },
                                    fontSize = 11.sp,
                                    color = FlareTextSecondary,
                                    maxLines = 1
                                )
                            }

                            if (targetUser.id == currentUser?.id) {
                                Text("(Anda)", fontSize = 12.sp, color = FlareTextSecondary)
                            } else if (isAlreadyFriend) {
                                Button(
                                    onClick = { onNavigateToChat(targetUser.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FlareOrange)
                                ) {
                                    Text("Chat", fontSize = 12.sp)
                                }
                            } else if (isPending) {
                                Text("Terkirim", fontSize = 12.sp, color = FlareTextSecondary)
                            } else {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.sendFriendRequest(targetUser.id)
                                            Toast.makeText(context, "Permintaan terkirim ke ${targetUser.displayName}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FlareOrange)
                                ) {
                                    Text("+ Tambah", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = FlareDarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, FlareCrimson.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Tidak ditemukan pengguna dengan ID: $searchIdQuery",
                            fontSize = 13.sp,
                            color = FlareCrimson,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        // Category Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Semua", "Online (${onlineFriends.size})", "Streak", "Permintaan (${incomingRequests.size})").forEach { cat ->
                val baseCategory = when {
                    cat.startsWith("Online") -> "Online"
                    cat.startsWith("Permintaan") -> "Permintaan"
                    else -> cat
                }
                val isSelected = selectedCategory == baseCategory
                Surface(
                    onClick = {
                        selectedCategory = baseCategory
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) FlareOrange else FlareDarkSurfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else FlareTextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Friends List
        if (displayedFriends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedCategory == "Permintaan") "Tidak ada permintaan pertemanan baru." else "Tidak ada teman dalam kategori ini.",
                    color = FlareTextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedFriends) { f ->
                    val isPendingIncoming = f.friendshipStatus == "PENDING_INCOMING"

                    Surface(
                        shape = RoundedCornerShape(18.dp),
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
                                size = 48.dp,
                                streakCount = f.streak?.currentStreak ?: 0,
                                isOnline = f.user.isOnline,
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
                                        OwnerGoldCheckmark(size = 14.dp)
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (f.user.isOnline) FlareGreen else Color(0xFF64748B))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = FlareRepository.formatLastSeen(f.user.isOnline, f.user.lastSeen),
                                        fontSize = 11.sp,
                                        fontWeight = if (f.user.isOnline) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (f.user.isOnline) FlareGreen else FlareTextTertiary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ID: ${f.user.sixDigitId}",
                                        fontSize = 11.sp,
                                        color = FlareTextSecondary
                                    )
                                }
                                if ((f.streak?.currentStreak ?: 0) > 0) {
                                    Text(
                                        text = "${f.streak?.currentStreak} Hari Streak 🔥",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = getStreakColor(f.streak?.currentStreak ?: 0)
                                    )
                                }
                            }

                            if (isPendingIncoming) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.respondFriendRequest(f.user.id, true)
                                                Toast.makeText(context, "Pertemanan diterima!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(FlareGreen.copy(alpha = 0.2f))
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Accept", tint = FlareGreen, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.respondFriendRequest(f.user.id, false)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(FlareCrimson.copy(alpha = 0.2f))
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Decline", tint = FlareCrimson, modifier = Modifier.size(18.dp))
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = { onNavigateToChat(f.user.id) },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(FlareDarkSurfaceVariant)
                                    ) {
                                        Icon(Icons.Default.ChatBubble, contentDescription = "Chat", tint = FlareOrange, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { friendToRemove = f.user },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(FlareCrimson.copy(alpha = 0.15f))
                                    ) {
                                        Icon(Icons.Default.PersonRemove, contentDescription = "Hapus Teman", tint = FlareCrimson, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Friend Confirmation Dialog
    if (friendToRemove != null) {
        val target = friendToRemove!!
        AlertDialog(
            onDismissRequest = { friendToRemove = null },
            containerColor = FlareDarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Hapus Pertemanan?",
                    fontWeight = FontWeight.Bold,
                    color = FlareTextPrimary,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus ${target.displayName} dari daftar teman? Api streak dan riwayat obrolan juga akan dihentikan.",
                    color = FlareTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.removeFriend(target.id)
                            friendToRemove = null
                            Toast.makeText(context, "${target.displayName} telah dihapus dari pertemanan", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlareCrimson)
                ) {
                    Text("Hapus Teman", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { friendToRemove = null }) {
                    Text("Batal", color = FlareTextSecondary)
                }
            }
        )
    }
}
