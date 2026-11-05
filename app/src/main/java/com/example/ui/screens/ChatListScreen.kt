package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.repository.ConversationWithFriend
import com.example.data.repository.FlareRepository
import com.example.ui.components.FlameBadge
import com.example.ui.components.FlareAvatar
import com.example.ui.components.OwnerGoldCheckmark
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    repository: FlareRepository,
    onSelectConversation: (friendId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val conversationsWithFriends by repository.getConversationsFlow().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var conversationToDelete by remember { mutableStateOf<ConversationWithFriend?>(null) }

    val filteredConversations = conversationsWithFriends.filter {
        it.friend.displayName.contains(searchQuery, ignoreCase = true) ||
        it.friend.username.contains(searchQuery, ignoreCase = true) ||
        it.friend.sixDigitId.contains(searchQuery)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlareDarkBackground)
            .padding(bottom = 90.dp)
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Percakapan",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = FlareTextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FlareDarkSurfaceVariant
                ) {
                    Text(
                        text = "${conversationsWithFriends.size} Chat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlareOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari chat, nama, atau ID 6 Digit...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = FlareTextSecondary)
                },
                modifier = Modifier.fillMaxWidth(),
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
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (filteredConversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = FlareTextTertiary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum ada percakapan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlareTextSecondary
                    )
                    Text(
                        text = "Buka tab Friends atau tambahkan ID 6 digit untuk mulai chat!",
                        fontSize = 13.sp,
                        color = FlareTextTertiary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredConversations) { item ->
                    val streakCount = item.streak?.currentStreak ?: 0
                    val timeStr = formatTime(item.conversation.lastMessageTime)

                    Surface(
                        onClick = { onSelectConversation(item.friend.id) },
                        shape = RoundedCornerShape(20.dp),
                        color = FlareDarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, FlareDarkCardBorder)
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
                                streakCount = streakCount,
                                isOnline = item.friend.isOnline,
                                isOwner = item.friend.isVerifiedOwner
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.friend.displayName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FlareTextPrimary
                                        )
                                        if (item.friend.isVerifiedOwner) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            OwnerGoldCheckmark(size = 15.dp)
                                        }
                                    }

                                    Text(
                                        text = timeStr,
                                        fontSize = 11.sp,
                                        color = FlareTextTertiary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.conversation.lastMessageText ?: "Mulai percakapan...",
                                        fontSize = 13.sp,
                                        color = FlareTextSecondary,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (streakCount > 0) {
                                        FlameBadge(streakCount = streakCount, size = 24.dp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    // Delete Chat Action Button
                                    IconButton(
                                        onClick = { conversationToDelete = item },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus Chat",
                                            tint = FlareTextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Conversation Confirmation Dialog
    if (conversationToDelete != null) {
        val target = conversationToDelete!!
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            containerColor = FlareDarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Hapus Percakapan?",
                    fontWeight = FontWeight.Bold,
                    color = FlareTextPrimary,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus seluruh riwayat chat dengan ${target.friend.displayName}? Seluruh pesan akan dihapus secara permanen.",
                    color = FlareTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteConversation(target.conversation.id)
                            conversationToDelete = null
                            Toast.makeText(context, "Percakapan berhasil dihapus", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlareCrimson)
                ) {
                    Text("Hapus Chat", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Batal", color = FlareTextSecondary)
                }
            }
        )
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
