package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Message
import com.example.data.model.Streak
import com.example.data.model.User
import com.example.data.repository.FlareRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    friendId: Long,
    repository: FlareRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val currentUser by repository.currentUser.collectAsState()
    val allUsers by repository.getAllUsersFlow().collectAsState(initial = emptyList())
    val friend = allUsers.find { it.id == friendId }

    val streaksWithUsers by repository.getStreaksFlow().collectAsState(initial = emptyList())
    val activeStreakItem = streaksWithUsers.find { it.friend.id == friendId }

    // Direct streak state from DB
    var streakState by remember { mutableStateOf<Streak?>(null) }
    var refreshStreakTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(friendId, refreshStreakTrigger) {
        streakState = repository.getStreakBetween(friendId)
    }

    val isFlameActive = streakState != null && streakState?.inviteStatus == "ACTIVE"
    val isPendingInvite = streakState?.inviteStatus == "PENDING_INVITE"
    val isInvitedByMe = streakState?.invitedByUserId == currentUser?.id

    // Retrieve or create conversation
    var conversationId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(friendId) {
        val conv = repository.getOrCreateConversation(friendId)
        conversationId = conv.id
    }

    val messages by produceState<List<Message>>(initialValue = emptyList(), conversationId) {
        val id = conversationId
        if (id != null) {
            repository.getMessagesFlow(id).collect { value = it }
        }
    }

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    var textInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var showE2EEDialog by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var selectedMessageForOptions by remember { mutableStateOf<Message?>(null) }
    var playingAudioId by remember { mutableStateOf<Long?>(null) }
    var showMenuDropdown by remember { mutableStateOf(false) }
    var showConfirmClearChatDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteChatDialog by remember { mutableStateOf(false) }
    var showConfirmUnfriendDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlareDarkBackground)
    ) {
        // Chat Header
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

                if (friend != null) {
                    FlareAvatar(
                        name = friend.displayName,
                        avatarIndex = friend.avatarIndex,
                        customPhotoUri = friend.customPhotoUri,
                        size = 42.dp,
                        streakCount = streakState?.currentStreak ?: 0,
                        isOnline = friend.isOnline,
                        isOwner = friend.isVerifiedOwner
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = friend.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlareTextPrimary
                            )
                            if (friend.isVerifiedOwner) {
                                Spacer(modifier = Modifier.width(4.dp))
                                OwnerGoldCheckmark(size = 15.dp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (friend.isOnline) FlareGreen else Color(0xFF64748B))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = FlareRepository.formatLastSeen(friend.isOnline, friend.lastSeen),
                                fontSize = 11.sp,
                                fontWeight = if (friend.isOnline) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (friend.isOnline) FlareGreen else FlareTextTertiary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•",
                                fontSize = 10.sp,
                                color = FlareTextTertiary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFlameActive) "Api ${streakState?.currentStreak}d 🔥" else "ID: ${friend.sixDigitId}",
                                fontSize = 11.sp,
                                color = if (isFlameActive) FlareOrange else FlareTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    E2EEBadge(onClick = { showE2EEDialog = true })

                    if (isFlameActive && (streakState?.currentStreak ?: 0) > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        FlameBadge(streakCount = streakState?.currentStreak ?: 0, size = 24.dp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Overflow Menu (Hapus Chat / Hapus Teman)
                    Box {
                        IconButton(onClick = { showMenuDropdown = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu Opsi",
                                tint = FlareTextPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false },
                            modifier = Modifier.background(FlareDarkSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bersihkan Isi Chat", color = FlareTextPrimary, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = FlareOrange, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showMenuDropdown = false
                                    showConfirmClearChatDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus Chat & Keluar", color = FlareCrimson, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = FlareCrimson, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showMenuDropdown = false
                                    showConfirmDeleteChatDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus Pertemanan", color = FlareCrimson, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = FlareCrimson, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showMenuDropdown = false
                                    showConfirmUnfriendDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Flame Invite Gatekeeper: Check if Flame has been accepted
        if (!isFlameActive) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = FlareOrange.copy(alpha = 0.6f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(FlareOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = FlareOrange,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isPendingInvite && !isInvitedByMe) {
                            // Current user received the invite from friend!
                            Text(
                                text = "Ajakan Api Masuk! 🔥",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = FlareTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${friend?.displayName} mengajakmu: 'Mau bikin api gak?'",
                                fontSize = 14.sp,
                                color = FlareOrangeLight,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Jika kamu memilih 'Ya', chat langsung dibuka dan api streak kalian akan menyala. Jika 'Tidak', chat akan otomatis dihapus.",
                                fontSize = 12.sp,
                                color = FlareTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // "Tidak" -> Chat automatically deleted
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.respondFlameInvite(friendId, accept = false)
                                            Toast.makeText(context, "Ajakan ditolak. Chat otomatis dihapus.", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FlareCrimson),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, FlareCrimson.copy(alpha = 0.6f))
                                ) {
                                    Text("Tidak", fontWeight = FontWeight.Bold)
                                }

                                // "Ya" -> Chat opens immediately!
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.respondFlameInvite(friendId, accept = true)
                                            refreshStreakTrigger++
                                            Toast.makeText(context, "Api berhasil dinyalakan! Selamat mengobrol 🔥", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FlareOrange)
                                ) {
                                    Text("Ya, Bikin Api! 🔥", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        } else if (isPendingInvite && isInvitedByMe) {
                            // User has already sent the invite, waiting for friend
                            Text(
                                text = "Ajakan Api Terkirim ⏳",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlareTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Kamu telah mengirim ajakan: 'Mau bikin api gak?' ke ${friend?.displayName}.",
                                fontSize = 13.sp,
                                color = FlareOrangeLight,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Menunggu temanmu memilih 'Ya' agar chat dapat dibuka secara penuh.",
                                fontSize = 11.sp,
                                color = FlareTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            // No invite sent yet
                            val isFriendOwner = friend?.role == "OWNER" || friend?.isVerifiedOwner == true
                            val amIOwner = currentUser?.role == "OWNER" || currentUser?.isVerifiedOwner == true

                            Text(
                                text = "Nyalakan Api Bersama",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = FlareTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (isFriendOwner && !amIOwner) {
                                // Rule: Regular users cannot invite owner
                                Text(
                                    text = "Pengguna tidak dapat membuat streak dengan Owner kecuali Owner yang mengirimkan pesan ajakan api terlebih dahulu.",
                                    fontSize = 12.sp,
                                    color = FlareGold,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            } else {
                                Text(
                                    text = "Sebelum mengobrol, kirimkan ajakan 'Mau bikin api gak?' ke ${friend?.displayName}.",
                                    fontSize = 13.sp,
                                    color = FlareTextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = repository.sendFlameInvite(friendId)
                                            res.onSuccess {
                                                refreshStreakTrigger++
                                                Toast.makeText(context, "Ajakan 'Mau bikin api gak?' berhasil dikirim!", Toast.LENGTH_SHORT).show()
                                            }.onFailure {
                                                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FlareOrange)
                                ) {
                                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Kirim Ajakan: Mau bikin api gak? 🔥",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Message List (Active Chat)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == currentUser?.id

                        ChatBubble(
                            message = msg,
                            isMe = isMe,
                            onLongClick = { selectedMessageForOptions = msg },
                            isPlayingAudio = playingAudioId == msg.id,
                            onToggleAudio = {
                                playingAudioId = if (playingAudioId == msg.id) null else msg.id
                            }
                        )
                    }
                }
            }

            // Reply quote indicator above input
            AnimatedVisibility(visible = replyingToMessage != null) {
                replyingToMessage?.let { rMsg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = FlareDarkSurfaceVariant,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(FlareOrange)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Membalas pesan:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FlareOrange
                                )
                                Text(
                                    text = rMsg.content,
                                    fontSize = 12.sp,
                                    color = FlareTextSecondary,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = { replyingToMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Batal", tint = FlareTextSecondary)
                            }
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = FlareDarkSurface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showAttachmentSheet = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(FlareDarkSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attachment",
                            tint = FlareOrange
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Ketik pesan terenkripsi...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlareOrange,
                            unfocusedBorderColor = FlareDarkCardBorder,
                            focusedTextColor = FlareTextPrimary,
                            unfocusedTextColor = FlareTextPrimary,
                            focusedContainerColor = FlareDarkSurfaceVariant,
                            unfocusedContainerColor = FlareDarkSurfaceVariant
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val text = textInput.trim()
                            if (text.isNotBlank()) {
                                val reply = replyingToMessage
                                textInput = ""
                                replyingToMessage = null
                                coroutineScope.launch {
                                    repository.sendMessage(
                                        receiverId = friendId,
                                        content = text,
                                        messageType = "TEXT",
                                        replyToId = reply?.id,
                                        replyToText = reply?.content
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(8.dp, CircleShape, ambientColor = FlareOrange, spotColor = FlareOrange)
                            .clip(CircleShape)
                            .background(FlareFlameGradient),
                        enabled = textInput.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Attachment Bottom Sheet Dialog
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = FlareDarkSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Bagikan Media & Berkas",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlareTextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOption(
                        icon = Icons.Default.Image,
                        title = "Foto",
                        color = FlareOrange
                    ) {
                        showAttachmentSheet = false
                        coroutineScope.launch {
                            repository.sendMessage(
                                receiverId = friendId,
                                content = "Foto liburan seru!",
                                messageType = "IMAGE",
                                mediaUrl = "sample_photo.jpg"
                            )
                        }
                    }

                    AttachmentOption(
                        icon = Icons.Default.Mic,
                        title = "Suara",
                        color = FlareCrimson
                    ) {
                        showAttachmentSheet = false
                        coroutineScope.launch {
                            repository.sendMessage(
                                receiverId = friendId,
                                content = "Pesan suara (0:24)",
                                messageType = "AUDIO",
                                mediaSize = "0:24"
                            )
                        }
                    }

                    AttachmentOption(
                        icon = Icons.Default.Videocam,
                        title = "Video",
                        color = FlarePurple
                    ) {
                        showAttachmentSheet = false
                        coroutineScope.launch {
                            repository.sendMessage(
                                receiverId = friendId,
                                content = "Video dokumentasi FLARE",
                                messageType = "VIDEO"
                            )
                        }
                    }

                    AttachmentOption(
                        icon = Icons.Default.InsertDriveFile,
                        title = "Dokumen",
                        color = FlareBlue
                    ) {
                        showAttachmentSheet = false
                        coroutineScope.launch {
                            repository.sendMessage(
                                receiverId = friendId,
                                content = "Panduan_Streak_Flare.pdf",
                                messageType = "FILE",
                                mediaName = "Panduan_Streak_Flare.pdf",
                                mediaSize = "2.4 MB"
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // E2EE Info Dialog
    if (showE2EEDialog) {
        AlertDialog(
            onDismissRequest = { showE2EEDialog = false },
            containerColor = FlareDarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enkripsi End-to-End Aktif",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlareTextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Pesan dan media dalam obrolan ini dilindungi dengan enkripsi ujung-ke-ujung (E2EE) 256-bit.",
                        fontSize = 13.sp,
                        color = FlareTextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FlareDarkSurfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Kode Verifikasi Keamanan:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlareTextSecondary
                            )
                            Text(
                                text = "8492 1048 5920 3841 • SHA-256",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showE2EEDialog = false }) {
                    Text("Tutup", color = FlareOrange, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Message Options Dialog (Reply, Copy, Delete)
    if (selectedMessageForOptions != null) {
        val selected = selectedMessageForOptions!!
        AlertDialog(
            onDismissRequest = { selectedMessageForOptions = null },
            containerColor = FlareDarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Opsi Pesan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlareTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            replyingToMessage = selected
                            selectedMessageForOptions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = null, tint = FlareOrange)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Balas Pesan", color = FlareTextPrimary)
                    }

                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Pesan", selected.content))
                            Toast.makeText(context, "Pesan disalin", Toast.LENGTH_SHORT).show()
                            selectedMessageForOptions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = FlareTextSecondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Salin Teks", color = FlareTextPrimary)
                    }

                    if (selected.senderId == currentUser?.id) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    repository.deleteMessage(selected.id)
                                    selectedMessageForOptions = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = FlareCrimson)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Hapus Pesan", color = FlareCrimson)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForOptions = null }) {
                    Text("Batal", color = FlareTextSecondary)
                }
            }
        )
    }

    // Confirm Clear Messages Dialog
    if (showConfirmClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearChatDialog = false },
            containerColor = FlareDarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Bersihkan Seluruh Pesan?", fontWeight = FontWeight.Bold, color = FlareTextPrimary, fontSize = 17.sp)
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus semua pesan dalam percakapan ini? Riwayat obrolan akan dikosongkan.",
                    color = FlareTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val id = conversationId
                            if (id != null) {
                                repository.clearMessagesForConversation(id)
                                Toast.makeText(context, "Isi obrolan berhasil dibersihkan", Toast.LENGTH_SHORT).show()
                            }
                            showConfirmClearChatDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlareOrange)
                ) {
                    Text("Bersihkan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearChatDialog = false }) {
                    Text("Batal", color = FlareTextSecondary)
                }
            }
        )
    }

    // Confirm Delete Conversation & Exit Dialog
    if (showConfirmDeleteChatDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteChatDialog = false },
            containerColor = FlareDarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Hapus Percakapan & Keluar?", fontWeight = FontWeight.Bold, color = FlareTextPrimary, fontSize = 17.sp)
            },
            text = {
                Text(
                    "Percakapan ini beserta seluruh pesannya akan dihapus secara permanen.",
                    color = FlareTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val id = conversationId
                            if (id != null) {
                                repository.deleteConversation(id)
                            }
                            showConfirmDeleteChatDialog = false
                            Toast.makeText(context, "Percakapan berhasil dihapus", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlareCrimson)
                ) {
                    Text("Hapus & Keluar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteChatDialog = false }) {
                    Text("Batal", color = FlareTextSecondary)
                }
            }
        )
    }

    // Confirm Unfriend Dialog
    if (showConfirmUnfriendDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmUnfriendDialog = false },
            containerColor = FlareDarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Hapus Pertemanan?", fontWeight = FontWeight.Bold, color = FlareTextPrimary, fontSize = 17.sp)
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus ${friend?.displayName} dari daftar teman? Api streak kalian juga akan dihentikan.",
                    color = FlareTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.removeFriend(friendId)
                            showConfirmUnfriendDialog = false
                            Toast.makeText(context, "${friend?.displayName} telah dihapus dari pertemanan", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlareCrimson)
                ) {
                    Text("Hapus Teman", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmUnfriendDialog = false }) {
                    Text("Batal", color = FlareTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = title, fontSize = 12.sp, color = FlareTextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ChatBubble(
    message: Message,
    isMe: Boolean,
    onLongClick: () -> Unit,
    isPlayingAudio: Boolean,
    onToggleAudio: () -> Unit
) {
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLongClick() },
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Reply Preview inside bubble
        if (!message.replyToText.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = FlareDarkSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(18.dp)
                            .background(FlareOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.replyToText,
                        fontSize = 11.sp,
                        color = FlareTextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        // Main Bubble Card
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            ),
            color = if (isMe) FlareOrange.copy(alpha = 0.9f) else FlareDarkSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isMe) FlareOrangeLight.copy(alpha = 0.4f) else FlareDarkCardBorder
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                // Audio Voice Note
                if (message.messageType == "AUDIO") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleAudio,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isMe) Color.White.copy(alpha = 0.2f) else FlareOrange.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = if (isMe) Color.White else FlareOrange
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(12, 20, 16, 24, 18, 14, 22, 10, 18, 24, 16, 12).forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(h.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (isPlayingAudio) FlareGold else (if (isMe) Color.White else FlareOrange)
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isPlayingAudio) "Memutar... 0:14" else "Pesan Suara (0:24)",
                                fontSize = 10.sp,
                                color = if (isMe) Color.White.copy(alpha = 0.8f) else FlareTextSecondary
                            )
                        }
                    }
                } else if (message.messageType == "IMAGE") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "📷 Foto Terenkripsi",
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        color = if (isMe) Color.White else FlareTextPrimary
                    )
                } else if (message.messageType == "FILE") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = if (isMe) Color.White else FlareOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = message.mediaName ?: message.content,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMe) Color.White else FlareTextPrimary
                            )
                            Text(
                                text = message.mediaSize ?: "Dokumen PDF",
                                fontSize = 11.sp,
                                color = if (isMe) Color.White.copy(alpha = 0.8f) else FlareTextSecondary
                            )
                        }
                    }
                } else {
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        color = if (isMe) Color.White else FlareTextPrimary,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.75f) else FlareTextTertiary
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            tint = if (message.status == "READ") FlareGold else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        if (message.reactions.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = FlareDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, FlareDarkCardBorder),
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = message.reactions.replace(",", " "),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
