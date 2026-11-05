package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import com.example.data.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        User::class,
        Friendship::class,
        Conversation::class,
        Message::class,
        Streak::class,
        NotificationItem::class,
        AuditLog::class,
        Announcement::class,
        UserAchievement::class,
        UserStatus::class,
        StatusView::class
    ],
    version = 4,
    exportSchema = false
)
abstract class FlareDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun friendshipDao(): FriendshipDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun streakDao(): StreakDao
    abstract fun notificationDao(): NotificationDao
    abstract fun adminDao(): AdminDao
    abstract fun achievementDao(): AchievementDao
    abstract fun statusDao(): StatusDao

    companion object {
        @Volatile
        private var INSTANCE: FlareDatabase? = null

        fun getDatabase(context: Context): FlareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlareDatabase::class.java,
                    "flare_social.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database)
                    }
                }
            }
        }

        suspend fun seedDatabase(db: FlareDatabase) {
            val userDao = db.userDao()
            val friendshipDao = db.friendshipDao()
            val conversationDao = db.conversationDao()
            val messageDao = db.messageDao()
            val streakDao = db.streakDao()
            val notificationDao = db.notificationDao()
            val adminDao = db.adminDao()
            val achievementDao = db.achievementDao()

            if (userDao.getUserByUsername("Zaky") != null) return

            val now = System.currentTimeMillis()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))

            // 1. Owner account Zaky
            val ownerId = userDao.insertUser(
                User(
                    username = "Zaky",
                    passwordHash = SecurityUtils.hashPassword("MUZAKY45"),
                    displayName = "Zaky Muzaky",
                    sixDigitId = "100001",
                    avatarIndex = 0,
                    bio = "Founder & Creator of FLARE 🔥 Keep The Flame Alive.",
                    role = "OWNER",
                    isVerifiedOwner = true,
                    isOnline = true,
                    lastSeen = now,
                    mustChangePassword = true,
                    joinedAt = now - (30L * 24 * 3600 * 1000)
                )
            )

            // 2. Demo Friend Alya (Online)
            val alyaId = userDao.insertUser(
                User(
                    username = "Alya",
                    passwordHash = SecurityUtils.hashPassword("alya123"),
                    displayName = "Alya Syakira",
                    sixDigitId = "238491",
                    avatarIndex = 1,
                    bio = "Aesthetic vibes & photography 📸✨",
                    role = "USER",
                    isVerifiedOwner = false,
                    isOnline = true,
                    lastSeen = now,
                    joinedAt = now - (20L * 24 * 3600 * 1000)
                )
            )

            // 3. Demo Friend Rian (Online)
            val rianId = userDao.insertUser(
                User(
                    username = "Rian",
                    passwordHash = SecurityUtils.hashPassword("rian123"),
                    displayName = "Rian Pratama",
                    sixDigitId = "482917",
                    avatarIndex = 2,
                    bio = "Gamer & software engineer 🎮 Let's streak everyday!",
                    role = "USER",
                    isVerifiedOwner = false,
                    isOnline = true,
                    lastSeen = now,
                    joinedAt = now - (25L * 24 * 3600 * 1000)
                )
            )

            // 4. Demo Friend Maya (Offline, 30m ago)
            val mayaId = userDao.insertUser(
                User(
                    username = "Maya",
                    passwordHash = SecurityUtils.hashPassword("maya123"),
                    displayName = "Maya Putri",
                    sixDigitId = "720481",
                    avatarIndex = 3,
                    bio = "Music and digital art lover 🎨🎵",
                    role = "USER",
                    isVerifiedOwner = false,
                    isOnline = false,
                    lastSeen = now - 1800000,
                    joinedAt = now - (15L * 24 * 3600 * 1000)
                )
            )

            // 5. Demo Friend Kelvin (Offline, 2 hours ago)
            val kelvinId = userDao.insertUser(
                User(
                    username = "Kelvin",
                    passwordHash = SecurityUtils.hashPassword("kelvin123"),
                    displayName = "Kelvin Wijaya",
                    sixDigitId = "619024",
                    avatarIndex = 4,
                    bio = "Always ready to connect! 🚀",
                    role = "USER",
                    isVerifiedOwner = false,
                    isOnline = false,
                    lastSeen = now - 7200000,
                    joinedAt = now - (5L * 24 * 3600 * 1000)
                )
            )

            // Friendships with Zaky
            friendshipDao.insertFriendship(Friendship(userId = ownerId, friendId = alyaId, status = "ACCEPTED"))
            friendshipDao.insertFriendship(Friendship(userId = ownerId, friendId = rianId, status = "ACCEPTED"))
            friendshipDao.insertFriendship(Friendship(userId = ownerId, friendId = mayaId, status = "ACCEPTED"))
            friendshipDao.insertFriendship(Friendship(userId = ownerId, friendId = kelvinId, status = "ACCEPTED"))

            // Streaks
            streakDao.insertStreak(
                Streak(
                    user1Id = ownerId,
                    user2Id = alyaId,
                    currentStreak = 7,
                    lastInteractionDate = todayStr,
                    user1SentToday = true,
                    user2SentToday = true,
                    highestStreak = 7,
                    inviteStatus = "ACTIVE"
                )
            )
            streakDao.insertStreak(
                Streak(
                    user1Id = ownerId,
                    user2Id = rianId,
                    currentStreak = 14,
                    lastInteractionDate = todayStr,
                    user1SentToday = true,
                    user2SentToday = false,
                    highestStreak = 14,
                    inviteStatus = "ACTIVE"
                )
            )
            // Kelvin has NO active streak with Zaky yet, so when Zaky chats with Kelvin (or vice versa), the flame invite rule triggers!

            // Conversations & Messages
            val convAlyaId = conversationDao.insertConversation(
                Conversation(
                    user1Id = ownerId,
                    user2Id = alyaId,
                    lastMessageText = "Keren banget fotonya! Keep the flame alive 🔥",
                    lastMessageTime = now - 1800000
                )
            )
            messageDao.insertMessage(
                Message(
                    conversationId = convAlyaId,
                    senderId = alyaId,
                    receiverId = ownerId,
                    content = "Hai Zaky! Jangan lupa kirim pesan hari ini biar streak 7 hari kita tetap jalan ya 🔥",
                    messageType = "TEXT",
                    timestamp = now - 3600000
                )
            )
            messageDao.insertMessage(
                Message(
                    conversationId = convAlyaId,
                    senderId = ownerId,
                    receiverId = alyaId,
                    content = "Siap Alya! Keren banget fotonya! Keep the flame alive 🔥",
                    messageType = "TEXT",
                    reactions = "🔥,❤️",
                    timestamp = now - 1800000
                )
            )

            val convRianId = conversationDao.insertConversation(
                Conversation(
                    user1Id = ownerId,
                    user2Id = rianId,
                    lastMessageText = "Gas mabar malam ini bro!",
                    lastMessageTime = now - 7200000
                )
            )
            messageDao.insertMessage(
                Message(
                    conversationId = convRianId,
                    senderId = rianId,
                    receiverId = ownerId,
                    content = "Streak 14 hari bro! Badge Burning Together unlocked! 💥",
                    messageType = "TEXT",
                    timestamp = now - 10800000
                )
            )
            messageDao.insertMessage(
                Message(
                    conversationId = convRianId,
                    senderId = ownerId,
                    receiverId = rianId,
                    content = "Gas mabar malam ini bro!",
                    messageType = "TEXT",
                    timestamp = now - 7200000
                )
            )

            // Seed User Achievements for Zaky
            achievementDao.insertAchievement(UserAchievement(userId = ownerId, achievementKey = FlareAchievements.FIRST_MESSAGE.key))
            achievementDao.insertAchievement(UserAchievement(userId = ownerId, achievementKey = FlareAchievements.FRIEND_FINDER.key))
            achievementDao.insertAchievement(UserAchievement(userId = ownerId, achievementKey = FlareAchievements.STREAK_STARTER.key))
            achievementDao.insertAchievement(UserAchievement(userId = ownerId, achievementKey = FlareAchievements.FLAME_KEEPER.key))

            // Notifications
            notificationDao.insertNotification(
                NotificationItem(
                    userId = ownerId,
                    title = "Prestasi Terbuka: Flame Keeper! 👑",
                    body = "Kamu berhasil mempertahankan api 7 hari berturut-turut!",
                    type = "ACHIEVEMENT",
                    timestamp = now - 3600000
                )
            )

            // Announcements
            adminDao.insertAnnouncement(
                Announcement(
                    title = "Selamat Datang di FLARE! 🔥",
                    content = "Keep The Flame Alive! Nikmati streak warna-warni tanpa batas, chat aman, pertemanan ID 6 digit, dan lencana prestasi profil.",
                    author = "Zaky (Owner)",
                    timestamp = now
                )
            )

            // Seed Initial Statuses
            val statusDao = db.statusDao()
            val zakyStatusId = statusDao.insertStatus(
                UserStatus(
                    userId = ownerId,
                    statusType = "TEXT",
                    content = "Selamat datang di FLARE! Keep The Flame Alive! Nyalakan api pertemananmu 🔥✨",
                    backgroundColorHex = "#FF5E00",
                    createdAt = now - 3600000,
                    expiresAt = now + 82800000
                )
            )
            statusDao.insertStatusView(StatusView(statusId = zakyStatusId, viewerUserId = alyaId, viewedAt = now - 1800000))
            statusDao.insertStatusView(StatusView(statusId = zakyStatusId, viewerUserId = rianId, viewedAt = now - 900000))

            val alyaStatusId = statusDao.insertStatus(
                UserStatus(
                    userId = alyaId,
                    statusType = "TEXT",
                    content = "Streak 7 hari sudah tercapai! Siapa yang mau bikin api baru hari ini? 🎨✨",
                    backgroundColorHex = "#8B5CF6",
                    createdAt = now - 7200000,
                    expiresAt = now + 79200000
                )
            )
            statusDao.insertStatusView(StatusView(statusId = alyaStatusId, viewerUserId = ownerId, viewedAt = now - 1000000))

            val rianStatusId = statusDao.insertStatus(
                UserStatus(
                    userId = rianId,
                    statusType = "TEXT",
                    content = "Mabar malam ini jam 8. Yang mau join langsung balas status ini ya! 🎮🔥",
                    backgroundColorHex = "#06B6D4",
                    createdAt = now - 10800000,
                    expiresAt = now + 75600000
                )
            )
        }
    }
}
