package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val displayName: String,
    val sixDigitId: String, // Unique 6-digit numeric ID e.g. "482917"
    val avatarIndex: Int = 0,
    val customPhotoUri: String? = null, // URI from local device Gallery
    val bio: String = "",
    val role: String = "USER", // "OWNER" or "USER"
    val isVerifiedOwner: Boolean = false,
    val isSuspended: Boolean = false,
    val isOnline: Boolean = true,
    val lastSeen: Long = System.currentTimeMillis(),
    val joinedAt: Long = System.currentTimeMillis(),
    val mustChangePassword: Boolean = false
)

@Entity(tableName = "friendships")
data class Friendship(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val friendId: Long,
    val status: String = "ACCEPTED", // "ACCEPTED", "PENDING_OUTGOING", "PENDING_INCOMING", "BLOCKED"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val user1Id: Long,
    val user2Id: Long,
    val lastMessageText: String? = null,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCountUser1: Int = 0,
    val unreadCountUser2: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val senderId: Long,
    val receiverId: Long,
    val content: String,
    val messageType: String = "TEXT", // "TEXT", "IMAGE", "VIDEO", "AUDIO", "FILE", "STICKER"
    val mediaUrl: String? = null,
    val mediaName: String? = null,
    val mediaSize: String? = null,
    val replyToId: Long? = null,
    val replyToText: String? = null,
    val reactions: String = "", // comma-separated e.g. "🔥,❤️"
    val status: String = "SENT", // "SENDING", "SENT", "DELIVERED", "READ"
    val isEncrypted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "streaks")
data class Streak(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val user1Id: Long,
    val user2Id: Long,
    val currentStreak: Int = 0,
    val lastInteractionDate: String = "", // "YYYY-MM-DD"
    val user1SentToday: Boolean = false,
    val user2SentToday: Boolean = false,
    val highestStreak: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val inviteStatus: String = "ACTIVE", // "ACTIVE", "PENDING_INVITE", "DECLINED", "NONE"
    val invitedByUserId: Long = 0
)

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val body: String,
    val type: String, // "MESSAGE", "FRIEND_REQUEST", "FRIEND_ACCEPTED", "FLAME_INVITE", "STREAK_UP", "STREAK_EXPIRING", "MILESTONE", "ACHIEVEMENT"
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val relatedUserId: Long? = null
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val adminUsername: String,
    val action: String,
    val target: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "announcements")
data class Announcement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val author: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "user_achievements")
data class UserAchievement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val achievementKey: String,
    val unlockedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_statuses")
data class UserStatus(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val statusType: String = "TEXT", // "TEXT", "IMAGE", "VIDEO"
    val content: String = "",        // Text message or caption / description
    val mediaUri: String? = null,    // Gallery photo or video URI
    val backgroundColorHex: String = "#FF5E00", // Background color for TEXT statuses
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours expiry
)

@Entity(tableName = "status_views")
data class StatusView(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val statusId: Long,
    val viewerUserId: Long,
    val viewedAt: Long = System.currentTimeMillis()
)

data class StatusWithUser(
    val status: UserStatus,
    val user: User,
    val viewCount: Int,
    val isViewedByMe: Boolean,
    val viewers: List<User> = emptyList()
)

data class AchievementDefinition(
    val key: String,
    val title: String,
    val description: String,
    val category: String,
    val iconEmoji: String,
    val tier: String // "Bronze", "Silver", "Gold", "Diamond", "Cosmic"
)

object FlareAchievements {
    val FIRST_MESSAGE = AchievementDefinition(
        key = "first_message_sent",
        title = "First Spark",
        description = "Kirim pesan pertama kamu untuk mengawali percakapan di FLARE.",
        category = "Komunikasi",
        iconEmoji = "💬",
        tier = "Bronze"
    )

    val FRIEND_FINDER = AchievementDefinition(
        key = "friend_finder",
        title = "Friend Finder",
        description = "Temukan kawan baru menggunakan pencarian ID 6 Digit.",
        category = "Sosial",
        iconEmoji = "🔍",
        tier = "Silver"
    )

    val CHATTERBOX = AchievementDefinition(
        key = "chatterbox",
        title = "Chatterbox",
        description = "Kirim lebih dari 10 pesan dalam percakapan aktif.",
        category = "Komunikasi",
        iconEmoji = "⚡",
        tier = "Gold"
    )

    val PROFILE_CUSTOMIZER = AchievementDefinition(
        key = "profile_customizer",
        title = "Profile Customizer",
        description = "Kustomisasi profilmu dengan foto galeri atau bio personal.",
        category = "Kustomisasi",
        iconEmoji = "🎨",
        tier = "Silver"
    )

    val STREAK_STARTER = AchievementDefinition(
        key = "streak_starter",
        title = "Streak Starter",
        description = "Mulai dan nyalakan api pertama bersama teman pilihanmu.",
        category = "Streak",
        iconEmoji = "🔥",
        tier = "Gold"
    )

    val MEDIA_PIONEER = AchievementDefinition(
        key = "media_pioneer",
        title = "Media Pioneer",
        description = "Bagikan media seperti foto galeri, dokumen, atau voice note.",
        category = "Media",
        iconEmoji = "📷",
        tier = "Diamond"
    )

    val FLAME_KEEPER = AchievementDefinition(
        key = "flame_keeper",
        title = "Flame Keeper",
        description = "Pertahankan api persahabatan selama 7 hari berturut-turut.",
        category = "Dedikasi",
        iconEmoji = "👑",
        tier = "Cosmic"
    )

    val all = listOf(
        FIRST_MESSAGE,
        FRIEND_FINDER,
        CHATTERBOX,
        PROFILE_CUSTOMIZER,
        STREAK_STARTER,
        MEDIA_PIONEER,
        FLAME_KEEPER
    )

    fun getByKey(key: String): AchievementDefinition? = all.find { it.key == key }
}

data class StreakMilestone(
    val days: Int,
    val title: String,
    val subtitle: String,
    val badgeLabel: String,
    val iconEmoji: String
)

object StreakMilestones {
    val list = listOf(
        StreakMilestone(1, "First Flame", "Api pertama menyala!", "First Flame", "🔥"),
        StreakMilestone(3, "Rising Flame", "Mulai memanas & konsisten!", "Rising Flame", "⚡"),
        StreakMilestone(7, "Hot Streak", "1 minggu bersama tanpa henti!", "Hot Streak", "🌟"),
        StreakMilestone(14, "Burning Together", "Dua minggu persahabatan erat!", "Burning Together", "💥"),
        StreakMilestone(30, "Flame Master", "1 bulan persahabatan legendaris!", "Flame Master", "👑"),
        StreakMilestone(50, "Legendary Flame", "Api abadi yang tak tergoyahkan!", "Legendary Flame", "🏆"),
        StreakMilestone(100, "Eternal Flame", "100 hari pertemanan luar biasa!", "Eternal Flame", "🌌"),
        StreakMilestone(365, "Immortal Flame", "1 tahun! Api kekal tak terkalahkan!", "Immortal Flame", "♾️"),
        StreakMilestone(500, "Solar Core Flame", "500 hari! Inti matahari persahabatan sejati!", "Solar Core", "☀️"),
        StreakMilestone(1000, "Galactic Supernova", "1000 hari! Api kosmik yang melampaui waktu!", "Supernova", "✨")
    )

    fun getMilestoneForDays(days: Int): StreakMilestone? {
        return list.lastOrNull { days >= it.days }
    }
}
