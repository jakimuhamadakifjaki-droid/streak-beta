package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLog
import com.example.data.model.User
import com.example.data.repository.FlareRepository
import com.example.data.repository.OwnerMetrics
import com.example.ui.components.FlareAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.OwnerGoldCheckmark
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    repository: FlareRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allUsers by repository.getAllUsersFlow().collectAsState(initial = emptyList())
    val announcements by repository.getAnnouncementsFlow().collectAsState(initial = emptyList())
    val auditLogs by repository.getAuditLogsFlow().collectAsState(initial = emptyList())

    var metrics by remember { mutableStateOf<OwnerMetrics?>(null) }
    LaunchedEffect(Unit) {
        metrics = repository.getOwnerMetrics()
    }

    var selectedTab by remember { mutableStateOf("Pengguna") } // "Pengguna", "Pengumuman", "Audit Log"
    var showCreateAnnouncementDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlareDarkBackground)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E1405),
            border = androidx.compose.foundation.BorderStroke(1.dp, FlareGold.copy(alpha = 0.3f))
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
                        tint = FlareGold
                    )
                }

                OwnerGoldCheckmark(size = 24.dp)

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "OWNER DASHBOARD",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = FlareGold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Panel Kontrol Khusus Pemilik & Verifikasi FLARE",
                        fontSize = 11.sp,
                        color = FlareGold.copy(alpha = 0.8f)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metrics Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Pengguna",
                        value = "${allUsers.size}",
                        icon = Icons.Default.Group,
                        color = FlareOrange,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Streak Aktif",
                        value = "${metrics?.totalActiveStreaks ?: 3}",
                        icon = Icons.Default.LocalFireDepartment,
                        color = FlareCrimson,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Percakapan",
                        value = "${metrics?.totalConversations ?: 4}",
                        icon = Icons.Default.ChatBubble,
                        color = FlarePurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Tab Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(FlareDarkSurfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Pengguna", "Pengumuman", "Audit Log").forEach { tab ->
                        val isSelected = selectedTab == tab
                        Button(
                            onClick = { selectedTab = tab },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) FlareGold else Color.Transparent,
                                contentColor = if (isSelected) Color(0xFF281C08) else FlareTextSecondary
                            ),
                            elevation = null,
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text(text = tab, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tab 1: User Management
            if (selectedTab == "Pengguna") {
                items(allUsers) { user ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = FlareDarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (user.isSuspended) FlareCrimson.copy(alpha = 0.5f) else FlareDarkCardBorder
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FlareAvatar(
                                    name = user.displayName,
                                    avatarIndex = user.avatarIndex,
                                    customPhotoUri = user.customPhotoUri,
                                    size = 44.dp,
                                    isOwner = user.isVerifiedOwner
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FlareTextPrimary
                                        )
                                        if (user.isVerifiedOwner) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            OwnerGoldCheckmark(size = 14.dp)
                                        }
                                    }
                                    Text(
                                        text = "ID: ${user.sixDigitId} • @${user.username} • Role: ${user.role}",
                                        fontSize = 11.sp,
                                        color = FlareTextSecondary
                                    )
                                    if (user.isSuspended) {
                                        Text(
                                            text = "Status: DITANGGUHKAN (BANNED)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FlareCrimson
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons per user
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (user.role != "OWNER") {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.toggleVerifiedOwnerBadge(user)
                                                Toast.makeText(context, "Status badge owner diubah", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (user.isVerifiedOwner) "Hapus Badge" else "Beri Centang Emas",
                                            fontSize = 11.sp,
                                            color = FlareGold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.toggleUserSuspension(user)
                                                val msg = if (user.isSuspended) "Akun diaktifkan kembali" else "Akun ditangguhkan"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (user.isSuspended) FlareGreen else FlareCrimson
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (user.isSuspended) "Buka Suspend" else "Suspend",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Akun Owner Utama (Dilindungi)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FlareGold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tab 2: Announcements
            if (selectedTab == "Pengumuman") {
                item {
                    Button(
                        onClick = { showCreateAnnouncementDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlareGold)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF281C08))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buat Pengumuman Baru (Siaran)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF281C08)
                        )
                    }
                }

                items(announcements) { ann ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = FlareDarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, FlareDarkCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = FlareGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = ann.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FlareTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = ann.content, fontSize = 12.sp, color = FlareTextSecondary, lineHeight = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Oleh: ${ann.author} • ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ann.timestamp))}",
                                fontSize = 10.sp,
                                color = FlareTextTertiary
                            )
                        }
                    }
                }
            }

            // Tab 3: Admin Audit Logs
            if (selectedTab == "Audit Log") {
                items(auditLogs) { log ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = FlareDarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, FlareDarkCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = log.action, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FlareGold)
                                Text(
                                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                                    fontSize = 11.sp,
                                    color = FlareTextTertiary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = log.details, fontSize = 12.sp, color = FlareTextSecondary)
                            Text(text = "Admin: ${log.adminUsername} | Target: ${log.target}", fontSize = 10.sp, color = FlareTextTertiary)
                        }
                    }
                }
            }
        }
    }

    // Create Announcement Modal
    if (showCreateAnnouncementDialog) {
        var annTitle by remember { mutableStateOf("") }
        var annContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateAnnouncementDialog = false },
            containerColor = FlareDarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(text = "Buat Pengumuman Siaran", fontWeight = FontWeight.Bold, color = FlareTextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = annTitle,
                        onValueChange = { annTitle = it },
                        label = { Text("Judul Pengumuman") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlareGold,
                            unfocusedBorderColor = FlareDarkCardBorder,
                            focusedTextColor = FlareTextPrimary,
                            unfocusedTextColor = FlareTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = annContent,
                        onValueChange = { annContent = it },
                        label = { Text("Isi Pengumuman") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlareGold,
                            unfocusedBorderColor = FlareDarkCardBorder,
                            focusedTextColor = FlareTextPrimary,
                            unfocusedTextColor = FlareTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.createAnnouncement(annTitle, annContent)
                            showCreateAnnouncementDialog = false
                            Toast.makeText(context, "Pengumuman berhasil disiarkan!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlareGold),
                    enabled = annTitle.isNotBlank() && annContent.isNotBlank()
                ) {
                    Text("Siarkan", color = Color(0xFF281C08), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAnnouncementDialog = false }) {
                    Text("Batal", color = FlareTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = FlareDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, FlareDarkCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = FlareTextPrimary)
            Text(text = title, fontSize = 11.sp, color = FlareTextSecondary)
        }
    }
}
