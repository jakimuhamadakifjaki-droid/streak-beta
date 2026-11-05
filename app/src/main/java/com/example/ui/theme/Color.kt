package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Flare Core Dark Theme Colors
val FlareDarkBackground = Color(0xFF090D16)
val FlareDarkSurface = Color(0xFF101726)
val FlareDarkSurfaceVariant = Color(0xFF1A2338)
val FlareDarkSurfaceContainer = Color(0xFF141D30)
val FlareDarkCardBorder = Color(0xFF24324D)

// Flare Vibrant Primary Accents
val FlareOrange = Color(0xFFFF5E00)
val FlareOrangeLight = Color(0xFFFF8533)
val FlareCrimson = Color(0xFFFF2A55)
val FlarePurple = Color(0xFFA855F7)
val FlareBlue = Color(0xFF00B4D8)
val FlareGreen = Color(0xFF10B981)
val FlareCyan = Color(0xFF06B6D4)
val FlarePink = Color(0xFFF72585)
val FlareGold = Color(0xFFFFD700)
val FlareGoldDark = Color(0xFFFF9E00)

// Text Colors
val FlareTextPrimary = Color(0xFFF8FAFC)
val FlareTextSecondary = Color(0xFF94A3B8)
val FlareTextTertiary = Color(0xFF64748B)

// Gradients
val FlareFlameGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFF2A55), Color(0xFFFF6200), Color(0xFFFFB703))
)

val FlareGoldGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFFDF00), Color(0xFFFFB703), Color(0xFFFFA500))
)

val FlareCosmicGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFF007A),
        Color(0xFF7928CA),
        Color(0xFF0070F3),
        Color(0xFF00DFD8)
    )
)

// Dynamic Multi-color Heat Spectrum for Streaks (Unlimited days, not rigid 1-day-1-color)
fun getStreakColor(streak: Int): Color {
    return when {
        streak <= 0 -> Color(0xFF64748B) // Inactive
        streak in 1..6 -> Color(0xFFFF6200) // Ignite Flame
        streak in 7..13 -> Color(0xFFFFB703) // Solar Blaze
        streak in 14..29 -> Color(0xFFA855F7) // Ultraviolet Plasma
        streak in 30..59 -> Color(0xFF00B4D8) // Oceanic Hyperfire
        streak in 60..99 -> Color(0xFF10B981) // Aurora Borealis
        streak in 100..364 -> Color(0xFFF72585) // Celestial Galaxy
        else -> Color(0xFFFFD700) // 365+ Immortal Supernova
    }
}

fun getStreakGradient(streak: Int): Brush {
    return when {
        streak <= 0 -> Brush.linearGradient(listOf(Color(0xFF475569), Color(0xFF64748B)))
        streak in 1..6 -> Brush.linearGradient(
            listOf(Color(0xFFFF3366), Color(0xFFFF6200), Color(0xFFFF9E00)) // Ember Ignite
        )
        streak in 7..13 -> Brush.linearGradient(
            listOf(Color(0xFFFF9E00), Color(0xFFFFD700), Color(0xFFFF4500)) // Solar Blaze
        )
        streak in 14..29 -> Brush.linearGradient(
            listOf(Color(0xFF7928CA), Color(0xFFFF007A), Color(0xFF00F2FE)) // Ultraviolet Plasma
        )
        streak in 30..59 -> Brush.linearGradient(
            listOf(Color(0xFF00C9FF), Color(0xFF92FE9D), Color(0xFF0072FF)) // Hyperfire Oceanic
        )
        streak in 60..99 -> Brush.linearGradient(
            listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFFFFD700)) // Prismatic Aurora
        )
        streak in 100..364 -> Brush.linearGradient(
            listOf(Color(0xFFFA709A), Color(0xFFFEE140), Color(0xFF9B51E0), Color(0xFF38BDF8)) // Celestial Galaxy
        )
        else -> Brush.linearGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFFFD700), Color(0xFFFF2A55), Color(0xFF8B5CF6)) // Immortal Supernova (365+)
        )
    }
}

// Distinct Styling for User Achievement Badges (Visually distinct from Streak Flames)
object AchievementBadges {
    val BronzeGradient = Brush.linearGradient(listOf(Color(0xFFB87333), Color(0xFFCD7F32), Color(0xFF8C5226)))
    val SilverGradient = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF94A3B8)))
    val GoldGradient = Brush.linearGradient(listOf(Color(0xFFFFE066), Color(0xFFFFB703), Color(0xFFD97706)))
    val DiamondGradient = Brush.linearGradient(listOf(Color(0xFF00F2FE), Color(0xFF4FACFE), Color(0xFF7928CA)))
    val CosmicGradient = Brush.linearGradient(listOf(Color(0xFFFF007A), Color(0xFF7928CA), Color(0xFF00F2FE)))
}
