package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.FlareRepository
import com.example.ui.components.FlareAvatar
import com.example.ui.components.OwnerGoldCheckmark
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: FlareRepository,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var isLoginTab by remember { mutableStateOf(true) }

    // Form inputs
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedAvatarIndex by remember { mutableIntStateOf(0) }
    var showPassword by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FlareDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Flare Emblem & Branding
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(16.dp, CircleShape, ambientColor = FlareOrange, spotColor = FlareCrimson)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(2.dp, FlareFlameGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_flare_logo),
                    contentDescription = "FLARE Logo",
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "FLARE",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = FlareOrange
            )

            Text(
                text = "Keep The Flame Alive.",
                fontSize = 14.sp,
                color = FlareTextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FlareDarkSurfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { isLoginTab = true; errorMessage = null },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLoginTab) FlareOrange else Color.Transparent,
                        contentColor = if (isLoginTab) Color.White else FlareTextSecondary
                    ),
                    elevation = null
                ) {
                    Text("Masuk", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { isLoginTab = false; errorMessage = null },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isLoginTab) FlareOrange else Color.Transparent,
                        contentColor = if (!isLoginTab) Color.White else FlareTextSecondary
                    ),
                    elevation = null
                ) {
                    Text("Daftar", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error display
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = FlareCrimson.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FlareCrimson.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = it,
                            color = Color(0xFFFF8595),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Input Fields
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = FlareOrange)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FlareOrange,
                    unfocusedBorderColor = FlareDarkCardBorder,
                    focusedTextColor = FlareTextPrimary,
                    unfocusedTextColor = FlareTextPrimary,
                    focusedLabelColor = FlareOrange,
                    unfocusedLabelColor = FlareTextSecondary,
                    focusedContainerColor = FlareDarkSurface,
                    unfocusedContainerColor = FlareDarkSurface
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (!isLoginTab) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nama Tampilan") },
                    leadingIcon = {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = FlareOrange)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlareOrange,
                        unfocusedBorderColor = FlareDarkCardBorder,
                        focusedTextColor = FlareTextPrimary,
                        unfocusedTextColor = FlareTextPrimary,
                        focusedLabelColor = FlareOrange,
                        unfocusedLabelColor = FlareTextSecondary,
                        focusedContainerColor = FlareDarkSurface,
                        unfocusedContainerColor = FlareDarkSurface
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Avatar Preset Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Pilih Avatar:",
                        fontSize = 13.sp,
                        color = FlareTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        (0..4).forEach { idx ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (selectedAvatarIndex == idx) 3.dp else 0.dp,
                                        color = if (selectedAvatarIndex == idx) FlareOrange else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedAvatarIndex = idx }
                            ) {
                                FlareAvatar(
                                    name = displayName.ifBlank { "User" },
                                    avatarIndex = idx,
                                    size = 44.dp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = FlareOrange)
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Password",
                            tint = FlareTextSecondary
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FlareOrange,
                    unfocusedBorderColor = FlareDarkCardBorder,
                    focusedTextColor = FlareTextPrimary,
                    unfocusedTextColor = FlareTextPrimary,
                    focusedLabelColor = FlareOrange,
                    unfocusedLabelColor = FlareTextSecondary,
                    focusedContainerColor = FlareDarkSurface,
                    unfocusedContainerColor = FlareDarkSurface
                )
            )

            if (!isLoginTab) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Konfirmasi Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = FlareOrange)
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlareOrange,
                        unfocusedBorderColor = FlareDarkCardBorder,
                        focusedTextColor = FlareTextPrimary,
                        unfocusedTextColor = FlareTextPrimary,
                        focusedLabelColor = FlareOrange,
                        unfocusedLabelColor = FlareTextSecondary,
                        focusedContainerColor = FlareDarkSurface,
                        unfocusedContainerColor = FlareDarkSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    errorMessage = null
                    isLoading = true
                    coroutineScope.launch {
                        if (isLoginTab) {
                            val res = repository.login(username, password)
                            isLoading = false
                            res.onSuccess { onAuthSuccess() }
                                .onFailure { errorMessage = it.message }
                        } else {
                            if (password != confirmPassword) {
                                isLoading = false
                                errorMessage = "Konfirmasi password tidak cocok."
                                return@launch
                            }
                            val res = repository.register(
                                username = username,
                                password = password,
                                displayName = displayName,
                                bio = bio,
                                avatarIndex = selectedAvatarIndex
                            )
                            isLoading = false
                            res.onSuccess { onAuthSuccess() }
                                .onFailure { errorMessage = it.message }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = FlareOrange, spotColor = FlareOrange),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlareOrange),
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (isLoginTab) "Masuk ke FLARE" else "Buat Akun & Dapatkan ID 6 Digit",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
