package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StatusWithUser
import com.example.data.model.User
import com.example.data.model.UserStatus
import com.example.data.repository.FlareRepository
import com.example.ui.components.FlareAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.OwnerGoldCheckmark
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusViewerScreen(
    statusItem: StatusWithUser,
    currentUser: User,
    repository: FlareRepository,
    onClose: () -> Unit,
    onNavigateToChat: (friendId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var replyText by remember { mutableStateOf("") }
    var isSendingReply by remember { mutableStateOf(false) }
    var showViewersSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isMyStatus = statusItem.status.userId == currentUser.id

    // Record view if not me
    LaunchedEffect(statusItem.status.id) {
        if (!isMyStatus) {
            repository.recordStatusView(statusItem.status.id)
        }
    }

    val status = statusItem.status
    val author = statusItem.user

    // Local image bitmap loader for gallery images
    var mediaBitmap by remember(status.mediaUri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(status.mediaUri) {
        if (status.statusType == "IMAGE" && !status.mediaUri.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(status.mediaUri)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val decoded = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (decoded != null) {
                        mediaBitmap = decoded.asImageBitmap()
                    }
                } catch (e: Exception) {
                    mediaBitmap = null
                }
            }
        }
    }

    val bgBrush = when (status.statusType) {
        "TEXT" -> {
            val color = try {
                Color(android.graphics.Color.parseColor(status.backgroundColorHex))
            } catch (e: Exception) {
                FlareOrange
            }
            Brush.verticalGradient(listOf(color.copy(alpha = 0.9f), FlareDarkBackground))
        }
        else -> Brush.verticalGradient(listOf(FlareDarkSurface, FlareDarkBackground))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Content Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush),
            contentAlignment = Alignment.Center
        ) {
            when (status.statusType) {
                "IMAGE" -> {
                    if (mediaBitmap != null) {
                        Image(
                            bitmap = mediaBitmap!!,
                            contentDescription = "Status Foto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Photo, contentDescription = null, tint = FlareOrange, modifier = Modifier.size(72.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Foto Status", color = FlareTextPrimary, fontSize = 16.sp)
                        }
                    }
                }
                "VIDEO" -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = FlareOrange.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(2.dp, FlareOrange),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = FlareOrange, modifier = Modifier.size(48.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Status Video Diputar",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = status.mediaUri ?: "Video dari Galeri",
                            color = FlareTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                else -> { // TEXT
                    Text(
                        text = status.content,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )
                }
            }
        }

        // Top Status Header (Author, Time, Close & Delete buttons)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Story Progress Bar
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = FlareOrange,
                trackColor = Color.White.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tutup", tint = Color.White)
                }

                FlareAvatar(
                    name = author.displayName,
                    avatarIndex = author.avatarIndex,
                    customPhotoUri = author.customPhotoUri,
                    size = 40.dp,
                    isOnline = author.isOnline,
                    isOwner = author.isVerifiedOwner
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = author.displayName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (author.isVerifiedOwner) {
                            Spacer(modifier = Modifier.width(4.dp))
                            OwnerGoldCheckmark(size = 14.dp)
                        }
                    }
                    Text(
                        text = formatStatusTime(status.createdAt),
                        color = FlareTextSecondary,
                        fontSize = 11.sp
                    )
                }

                if (isMyStatus || currentUser.role == "OWNER") {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus Status", tint = FlareCrimson)
                    }
                }
            }
        }

        // Bottom Caption & Reply Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Caption / Description for Image & Video
            if ((status.statusType == "IMAGE" || status.statusType == "VIDEO") && status.content.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = status.content,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (isMyStatus) {
                // My status view count bar
                Surface(
                    onClick = { showViewersSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    color = FlareDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FlareOrange.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = FlareOrange, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dilihat oleh ${statusItem.viewCount} orang",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Lihat Penonton ❯",
                            color = FlareOrangeLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                // Reply to Status bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Balas status ke ${author.displayName}...", color = FlareTextTertiary, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlareOrange,
                            unfocusedBorderColor = FlareDarkCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = FlareDarkSurface.copy(alpha = 0.9f),
                            unfocusedContainerColor = FlareDarkSurface.copy(alpha = 0.9f)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank() && !isSendingReply) {
                                isSendingReply = true
                                coroutineScope.launch {
                                    val res = repository.replyToStatus(status, author, replyText)
                                    isSendingReply = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Balasan terkirim ke chat pribadi!", Toast.LENGTH_SHORT).show()
                                        replyText = ""
                                        onClose()
                                        onNavigateToChat(author.id)
                                    } else {
                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Gagal membalas", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = replyText.isNotBlank() && !isSendingReply,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (replyText.isNotBlank()) FlareOrange else FlareDarkSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim Balasan",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Viewers Modal Sheet
    if (showViewersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showViewersSheet = false },
            containerColor = FlareDarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dilihat oleh (${statusItem.viewers.size} Orang)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlareTextPrimary
                    )
                    IconButton(onClick = { showViewersSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = FlareTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (statusItem.viewers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada yang melihat status ini.",
                            color = FlareTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(statusItem.viewers) { viewer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(FlareDarkSurfaceVariant)
                                    .clickable {
                                        showViewersSheet = false
                                        onClose()
                                        onNavigateToChat(viewer.id)
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FlareAvatar(
                                    name = viewer.displayName,
                                    avatarIndex = viewer.avatarIndex,
                                    customPhotoUri = viewer.customPhotoUri,
                                    size = 40.dp,
                                    isOnline = viewer.isOnline,
                                    isOwner = viewer.isVerifiedOwner
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = viewer.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FlareTextPrimary
                                        )
                                        if (viewer.isVerifiedOwner) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            OwnerGoldCheckmark(size = 14.dp)
                                        }
                                    }
                                    Text(
                                        text = "ID: ${viewer.sixDigitId}",
                                        fontSize = 11.sp,
                                        color = FlareTextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Chat",
                                    tint = FlareOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = FlareDarkSurface,
            title = { Text("Hapus Status Ini?", color = FlareTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Status ini akan dihapus secara permanen dan tidak dapat dilihat lagi.", color = FlareTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteStatus(status.id)
                            showDeleteConfirm = false
                            onClose()
                            Toast.makeText(context, "Status telah dihapus", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlareCrimson)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", color = FlareTextSecondary)
                }
            }
        )
    }
}

// Dialog / Sheet to create a new Status
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStatusBottomSheet(
    repository: FlareRepository,
    onDismiss: () -> Unit,
    onStatusCreated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var statusType by remember { mutableStateOf("TEXT") } // "TEXT", "IMAGE", "VIDEO"
    var contentText by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<String?>(null) }
    var selectedColorHex by remember { mutableStateOf("#FF5E00") }
    var isSubmitting by remember { mutableStateOf(false) }

    val bgColors = listOf("#FF5E00", "#8B5CF6", "#06B6D4", "#10B981", "#EF4444", "#D946EF", "#1E293B")

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedMediaUri = uri.toString()
            statusType = "IMAGE"
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedMediaUri = uri.toString()
            statusType = "VIDEO"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FlareDarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Buat Status Baru 🔥",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlareTextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = FlareTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selector Tabs: Teks, Foto, Video
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("TEXT", "Teks ✍️", Icons.Default.Edit),
                    Triple("IMAGE", "Foto 📷", Icons.Default.PhotoCamera),
                    Triple("VIDEO", "Video 🎥", Icons.Default.Videocam)
                ).forEach { (type, label, icon) ->
                    val isSelected = statusType == type
                    Surface(
                        onClick = {
                            statusType = type
                            if (type == "IMAGE") {
                                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else if (type == "VIDEO") {
                                videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) FlareOrange else FlareDarkSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else FlareTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else FlareTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Preview or Color Palette
            if (statusType == "TEXT") {
                // Background color palette
                Text("Pilih Warna Latar:", fontSize = 12.sp, color = FlareTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(bgColors) { hex ->
                        val isSelected = selectedColorHex == hex
                        val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { FlareOrange }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    placeholder = { Text("Ketik status yang ingin kamu bagikan...", color = FlareTextTertiary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlareOrange,
                        unfocusedBorderColor = FlareDarkCardBorder,
                        focusedTextColor = FlareTextPrimary,
                        unfocusedTextColor = FlareTextPrimary,
                        focusedContainerColor = FlareDarkSurfaceVariant,
                        unfocusedContainerColor = FlareDarkSurfaceVariant
                    )
                )
            } else {
                // Media preview / picker button
                Surface(
                    onClick = {
                        if (statusType == "IMAGE") {
                            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else {
                            videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = FlareDarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FlareOrange.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (statusType == "IMAGE") Icons.Default.AddPhotoAlternate else Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = FlareOrange,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedMediaUri != null) "Media Terpilih ✅ (Klik untuk ganti)" else "Pilih ${if (statusType == "IMAGE") "Foto" else "Video"} dari Galeri",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Deskripsi status
                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    placeholder = { Text("Tambahkan deskripsi status (opsional)...", color = FlareTextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlareOrange,
                        unfocusedBorderColor = FlareDarkCardBorder,
                        focusedTextColor = FlareTextPrimary,
                        unfocusedTextColor = FlareTextPrimary,
                        focusedContainerColor = FlareDarkSurfaceVariant,
                        unfocusedContainerColor = FlareDarkSurfaceVariant
                    ),
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Post Status Button
            Button(
                onClick = {
                    if (statusType == "TEXT" && contentText.isBlank()) {
                        Toast.makeText(context, "Ketik status Anda terlebih dahulu", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (statusType != "TEXT" && selectedMediaUri.isNullOrBlank()) {
                        Toast.makeText(context, "Silakan pilih foto atau video terlebih dahulu", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true
                    coroutineScope.launch {
                        val result = repository.createStatus(
                            statusType = statusType,
                            content = contentText.trim(),
                            mediaUri = selectedMediaUri,
                            backgroundColorHex = selectedColorHex
                        )
                        isSubmitting = false
                        if (result.isSuccess) {
                            Toast.makeText(context, "Status berhasil dipublikasikan! 🔥", Toast.LENGTH_SHORT).show()
                            onStatusCreated()
                        } else {
                            Toast.makeText(context, result.exceptionOrNull()?.message ?: "Gagal membuat status", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlareOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bagikan ke Status", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private fun formatStatusTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = (diff / (1000 * 60)).toInt()
    return when {
        mins < 1 -> "Baru saja"
        mins < 60 -> "$mins menit yang lalu"
        mins < 1440 -> "${mins / 60} jam yang lalu"
        else -> {
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
