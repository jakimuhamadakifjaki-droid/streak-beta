package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FlareDarkSurface
import com.example.ui.theme.getStreakGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val avatarGradients = listOf(
    Brush.linearGradient(listOf(Color(0xFFFF5E00), Color(0xFFFF2A55))),
    Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))),
    Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))),
    Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
    Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444))),
    Brush.linearGradient(listOf(Color(0xFFD946EF), Color(0xFF8B5CF6)))
)

@Composable
fun FlareAvatar(
    name: String,
    avatarIndex: Int = 0,
    customPhotoUri: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    streakCount: Int = 0,
    isOnline: Boolean = false,
    isOwner: Boolean = false
) {
    val context = LocalContext.current
    val gradient = avatarGradients[avatarIndex.coerceIn(0, avatarGradients.size - 1)]
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "F"

    var bitmap by remember(customPhotoUri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(customPhotoUri) {
        if (!customPhotoUri.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(customPhotoUri)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val decoded = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (decoded != null) {
                        bitmap = decoded.asImageBitmap()
                    }
                } catch (e: Exception) {
                    bitmap = null
                }
            }
        } else {
            bitmap = null
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing ring if active streak exists or is owner
        val ringModifier = if (streakCount > 0) {
            Modifier
                .fillMaxSize()
                .border(width = 2.5.dp, brush = getStreakGradient(streakCount), shape = CircleShape)
                .padding(3.dp)
        } else if (isOwner) {
            Modifier
                .fillMaxSize()
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFF8800))),
                    shape = CircleShape
                )
                .padding(3.dp)
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = ringModifier
                .clip(CircleShape)
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.42f).sp
                )
            }
        }

        // Online status dot
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .border(2.dp, FlareDarkSurface, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
            )
        }
    }
}
