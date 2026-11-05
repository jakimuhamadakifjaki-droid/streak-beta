package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AchievementDefinition
import com.example.data.model.FlareAchievements
import com.example.data.model.User
import com.example.data.repository.FlareRepository
import com.example.ui.components.AchievementBadgeItem
import com.example.ui.components.FlareAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.OwnerGoldCheckmark
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    currentUser: User,
    repository: FlareRepository,
    onNavigateToOwnerDashboard: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val friendsList by repository.getFriendsForCurrentUser().collectAsState(initial = emptyList())
    val streaksList by repository.getStreaksFlow().collectAsState(initial = emptyList())
    val allUsers by repository.getAllUsersFlow().collectAsState(initial = emptyList())
    val userAchievements by repository.getUserAchievementsFlow(currentUser.id).collectAsState(initial = emptyList())

    val totalFriends = friendsList.count { it.friendshipStatus == "ACCEPTED" }
    val activeStreaks = streaksList.count { it.streak.currentStreak > 0 }
    val maxStreak = streaksList.maxOfOrNull { it.streak.highestStreak } ?: 0

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showAccountSwitcher by remember { mutableStateOf(false) }
    var selectedAchievementForDetail by remember { mutableStateOf<AchievementDefinition?>(null) }

    // Android Zero-permission Photo Picker for Gallery Image
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                repository.updateProfile(
                    displayName = currentUser.displayName,
                    bio = currentUser.bio,
                    avatarIndex = currentUser.avatarIndex,
                    customPhotoUri = uri.toString()
                )
                Toast.makeText(context, "Foto profil dari galeri berhasil dipasang!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlareDarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profil Saya",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = FlareTextPrimary
            )

            IconButton(
                onClick = { showAccountSwitcher = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(FlareDarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.SwitchAccount,
                    contentDescription = "Switch Account",
                    tint = FlareOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Profile Avatar & Identity Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            cornerRadius = 24.dp,
            borderColor = if (currentUser.isVerifiedOwner) FlareGold.copy(alpha = 0.5f) else FlareDarkCardBorder
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    FlareAvatar(
                        name = currentUser.displayName,
                        avatarIndex = currentUser.avatarIndex,
                        customPhotoUri = currentUser.customPhotoUri,
                        size = 86.dp,
                        streakCount = maxStreak,
                        isOwner = currentUser.isVerifiedOwner
                    )

                    // Quick Gallery Pick Badge Button
                    Surface(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = CircleShape,
                        color = FlareOrange,
                        modifier = Modifier.size(28.dp),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Pilih Foto Galeri",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentUser.displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlareTextPrimary
                    )
                    if (currentUser.isVerifiedOwner) {
                        Spacer(modifier = Modifier.width(6.dp))
                        OwnerGoldCheckmark(size = 20.dp)
                    }
                }

                Text(
                    text = "@${currentUser.username}",
                    fontSize = 13.sp,
                    color = FlareTextSecondary
                )

                // Online / Offline Status Toggle Badge
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            val newStatus = !currentUser.isOnline
                            repository.toggleMyOnlineStatus(newStatus)
                            Toast.makeText(
                                context,
                                if (newStatus) "Status Anda kini Online 🟢" else "Status Anda kini Offline ⚪",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (currentUser.isOnline) FlareGreen.copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (currentUser.isOnline) FlareGreen.copy(alpha = 0.4f) else Color(0xFF64748B).copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (currentUser.isOnline) FlareGreen else Color(0xFF64748B))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentUser.isOnline) "Status: Online (Klik untuk Ubah)" else "Status: Offline (Klik untuk Ubah)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentUser.isOnline) FlareGreen else FlareTextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prominent 6-Digit ID Container with Copy Button
                Surface(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("FLARE ID", currentUser.sixDigitId))
                        Toast.makeText(context, "ID ${currentUser.sixDigitId} berhasil disalin!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = FlareDarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FlareOrange.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            tint = FlareOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FLARE ID: ${currentUser.sixDigitId}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlareOrangeLight
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin ID",
                            tint = FlareOrangeLight,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentUser.bio.ifBlank { "Belum ada bio. Keep The Flame Alive!" },
                    fontSize = 13.sp,
                    color = FlareTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FlareDarkSurfaceVariant)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "Teman", value = "$totalFriends")
                    StatItem(label = "Streak Aktif", value = "$activeStreaks")
                    StatItem(label = "Tertinggi", value = "${maxStreak}d 🔥")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Owner Exclusive Dashboard Button
        if (currentUser.role == "OWNER" || currentUser.isVerifiedOwner) {
            Surface(
                onClick = onNavigateToOwnerDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF281C08),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, FlareGold.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OwnerGoldCheckmark(size = 28.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Owner Dashboard",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlareGold
                        )
                        Text(
                            text = "Kelola pengguna, status suspensi, dan pengumuman aplikasi",
                            fontSize = 11.sp,
                            color = FlareGold.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = FlareGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Expanded User Achievements & Badges Section
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = FlareGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Prestasi & Lencana",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlareTextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FlareDarkSurfaceVariant
                ) {
                    Text(
                        text = "${userAchievements.size} / ${FlareAchievements.all.size} Terbuka",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlareOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlareAchievements.all.forEach { ach ->
                    val userAch = userAchievements.find { it.achievementKey == ach.key }
                    val isUnlocked = userAch != null

                    AchievementBadgeItem(
                        achievement = ach,
                        isUnlocked = isUnlocked,
                        unlockedAt = userAch?.unlockedAt,
                        onClick = { selectedAchievementForDetail = ach }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileActionButton(
                icon = Icons.Default.AddPhotoAlternate,
                title = "Ganti Foto dari Galeri",
                subtitle = "Pilih foto profil langsung dari penyimpanan perangkat",
                iconColor = FlareBlue
            ) {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }

            ProfileActionButton(
                icon = Icons.Default.Edit,
                title = "Edit Bio & Nama",
                subtitle = "Ubah nama tampilan, bio, atau avatar preset"
            ) {
                showEditProfileDialog = true
            }

            ProfileActionButton(
                icon = Icons.Default.Key,
                title = "Ganti Password",
                subtitle = if (currentUser.mustChangePassword) "Wajib diubah (Kredensial awal)" else "Perbarui kata sandi akun"
            ) {
                showChangePasswordDialog = true
            }

            ProfileActionButton(
                icon = Icons.Default.Logout,
                title = "Keluar (Logout)",
                subtitle = "Akhiri sesi login perangkat",
                iconColor = FlareCrimson
            ) {
                repository.logout()
                onLogout()
            }
        }
    }

    // Achievement Detail Dialog
    if (selectedAchievementForDetail != null) {
        val ach = selectedAchievementForDetail!!
        val isUnlocked = userAchievements.any { it.achievementKey == ach.key }

        AlertDialog(
            onDismissRequest = { selectedAchievementForDetail = null },
            containerColor = FlareDarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = ach.iconEmoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = ach.title, fontWeight = FontWeight.Bold, color = FlareTextPrimary, fontSize = 16.sp)
                        Text(text = "Tingkat: ${ach.tier} • Kategori: ${ach.category}", fontSize = 11.sp, color = FlareOrange)
                    }
                }
            },
            text = {
                Column {
                    Text(
                        text = ach.description,
                        fontSize = 13.sp,
                        color = FlareTextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUnlocked) FlareGreen.copy(alpha = 0.15f) else FlareDarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isUnlocked) FlareGreen.copy(alpha = 0.5f) else FlareDarkCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isUnlocked) FlareGreen else FlareTextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isUnlocked) "Lencana Telah Berhasil Diraih!" else "Belum Terbuka — Selesaikan tugas untuk meraih lencana ini.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isUnlocked) FlareGreen else FlareTextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAchievementForDetail = null }) {
                    Text("Tutup", color = FlareOrange, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Edit Profile Modal
    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(currentUser.displayName) }
        var editBio by remember { mutableStateOf(currentUser.bio) }
        var editAvatar by remember { mutableIntStateOf(currentUser.avatarIndex) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = FlareDarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(text = "Edit Profil", fontWeight = FontWeight.Bold, color = FlareTextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nama Tampilan") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlareOrange,
                            unfocusedBorderColor = FlareDarkCardBorder,
                            focusedTextColor = FlareTextPrimary,
                            unfocusedTextColor = FlareTextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlareOrange,
                            unfocusedBorderColor = FlareDarkCardBorder,
                            focusedTextColor = FlareTextPrimary,
                            unfocusedTextColor = FlareTextPrimary
                        )
                    )

                    Text(text = "Preset Avatar Alternatif:", fontSize = 12.sp, color = FlareTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        (0..4).forEach { idx ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (editAvatar == idx) 2.5.dp else 0.dp,
                                        color = FlareOrange,
                                        shape = CircleShape
                                    )
                                    .clickable { editAvatar = idx }
                            ) {
                                FlareAvatar(name = editName, avatarIndex = idx, size = 38.dp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.updateProfile(editName, editBio, editAvatar)
                            showEditProfileDialog = false
                            Toast.makeText(context, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlareOrange)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Batal", color = FlareTextSecondary)
                }
            }
        )
    }

    // Change Password Modal
    if (showChangePasswordDialog) {
        var oldPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var passError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            containerColor = FlareDarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(text = "Ganti Password", fontWeight = FontWeight.Bold, color = FlareTextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (passError != null) {
                        Text(text = passError!!, color = FlareCrimson, fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it },
                        label = { Text("Password Saat Ini") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlareOrange,
                            unfocusedBorderColor = FlareDarkCardBorder,
                            focusedTextColor = FlareTextPrimary,
                            unfocusedTextColor = FlareTextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Password Baru (min 6 karakter)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlareOrange,
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
                            val res = repository.changePassword(oldPass, newPass)
                            res.onSuccess {
                                showChangePasswordDialog = false
                                Toast.makeText(context, "Password berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                passError = it.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlareOrange),
                    enabled = oldPass.isNotBlank() && newPass.length >= 6
                ) {
                    Text("Perbarui", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Batal", color = FlareTextSecondary)
                }
            }
        )
    }

    // Account Switcher Dialog
    if (showAccountSwitcher) {
        AlertDialog(
            onDismissRequest = { showAccountSwitcher = false },
            containerColor = FlareDarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(text = "Ganti Akun", fontWeight = FontWeight.Bold, color = FlareTextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pilih akun yang ingin Anda gunakan:",
                        fontSize = 12.sp,
                        color = FlareTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    allUsers.forEach { user ->
                        Surface(
                            onClick = {
                                repository.switchAccount(user)
                                showAccountSwitcher = false
                                Toast.makeText(context, "Beralih ke akun ${user.displayName}", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (user.id == currentUser.id) FlareOrange.copy(alpha = 0.2f) else FlareDarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (user.id == currentUser.id) FlareOrange else FlareDarkCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FlareAvatar(
                                    name = user.displayName,
                                    avatarIndex = user.avatarIndex,
                                    customPhotoUri = user.customPhotoUri,
                                    size = 36.dp,
                                    isOwner = user.isVerifiedOwner
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = user.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FlareTextPrimary)
                                        if (user.isVerifiedOwner) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            OwnerGoldCheckmark(size = 14.dp)
                                        }
                                    }
                                    Text(text = "ID: ${user.sixDigitId} • @${user.username}", fontSize = 11.sp, color = FlareTextSecondary)
                                }
                                if (user.id == currentUser.id) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = FlareOrange, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountSwitcher = false }) {
                    Text("Tutup", color = FlareOrange)
                }
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = FlareOrange)
        Text(text = label, fontSize = 11.sp, color = FlareTextSecondary)
    }
}

@Composable
private fun ProfileActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = FlareOrange,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FlareTextPrimary)
                Text(text = subtitle, fontSize = 11.sp, color = FlareTextSecondary)
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = FlareTextTertiary
            )
        }
    }
}
