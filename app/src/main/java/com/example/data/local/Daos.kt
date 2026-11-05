package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserByIdFlow(id: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): User?

    @Query("SELECT * FROM users WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE sixDigitId = :sixDigitId LIMIT 1")
    suspend fun getUserBySixDigitId(sixDigitId: String): User?

    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET isOnline = :isOnline, lastSeen = :lastSeen WHERE id = :userId")
    suspend fun updateOnlineStatus(userId: Long, isOnline: Boolean, lastSeen: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteUser(user: User)
}

@Dao
interface FriendshipDao {
    @Query("SELECT * FROM friendships WHERE userId = :userId OR friendId = :userId")
    fun getFriendshipsFlow(userId: Long): Flow<List<Friendship>>

    @Query("SELECT * FROM friendships WHERE (userId = :u1 AND friendId = :u2) OR (userId = :u2 AND friendId = :u1) LIMIT 1")
    suspend fun getFriendship(u1: Long, u2: Long): Friendship?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendship(friendship: Friendship): Long

    @Update
    suspend fun updateUserFriendship(friendship: Friendship)

    @Query("DELETE FROM friendships WHERE (userId = :u1 AND friendId = :u2) OR (userId = :u2 AND friendId = :u1)")
    suspend fun deleteFriendship(u1: Long, u2: Long)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE user1Id = :userId OR user2Id = :userId ORDER BY lastMessageTime DESC")
    fun getConversationsFlow(userId: Long): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE (user1Id = :u1 AND user2Id = :u2) OR (user1Id = :u2 AND user2Id = :u1) LIMIT 1")
    suspend fun getConversationBetween(u1: Long, u2: Long): Conversation?

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: Long): Conversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation): Long

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    @Query("DELETE FROM conversations WHERE (user1Id = :u1 AND user2Id = :u2) OR (user1Id = :u2 AND user2Id = :u1)")
    suspend fun deleteConversationBetween(u1: Long, u2: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesFlow(conversationId: Long): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Update
    suspend fun updateMessage(message: Message)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE senderId = :userId")
    suspend fun countMessagesBySender(userId: Long): Int
}

@Dao
interface StreakDao {
    @Query("SELECT * FROM streaks WHERE user1Id = :userId OR user2Id = :userId")
    fun getStreaksFlow(userId: Long): Flow<List<Streak>>

    @Query("SELECT * FROM streaks WHERE (user1Id = :u1 AND user2Id = :u2) OR (user1Id = :u2 AND user2Id = :u1) LIMIT 1")
    suspend fun getStreakBetween(u1: Long, u2: Long): Streak?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: Streak): Long

    @Update
    suspend fun updateStreak(streak: Streak)

    @Query("DELETE FROM streaks WHERE (user1Id = :u1 AND user2Id = :u2) OR (user1Id = :u2 AND user2Id = :u1)")
    suspend fun deleteStreakBetween(u1: Long, u2: Long)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsFlow(userId: Long): Flow<List<NotificationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: Long)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun clearAll(userId: Long)
}

@Dao
interface AdminDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAuditLogsFlow(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog): Long

    @Query("SELECT * FROM announcements ORDER BY timestamp DESC")
    fun getAnnouncementsFlow(): Flow<List<Announcement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: Announcement): Long
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM user_achievements WHERE userId = :userId ORDER BY unlockedAt DESC")
    fun getUserAchievementsFlow(userId: Long): Flow<List<UserAchievement>>

    @Query("SELECT * FROM user_achievements WHERE userId = :userId AND achievementKey = :key LIMIT 1")
    suspend fun getAchievement(userId: Long, key: String): UserAchievement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: UserAchievement): Long
}

@Dao
interface StatusDao {
    @Query("SELECT * FROM user_statuses WHERE expiresAt > :now ORDER BY createdAt DESC")
    fun getActiveStatusesFlow(now: Long = System.currentTimeMillis()): Flow<List<UserStatus>>

    @Query("SELECT * FROM user_statuses WHERE userId = :userId AND expiresAt > :now ORDER BY createdAt DESC")
    fun getUserActiveStatusesFlow(userId: Long, now: Long = System.currentTimeMillis()): Flow<List<UserStatus>>

    @Query("SELECT * FROM user_statuses WHERE id = :statusId LIMIT 1")
    suspend fun getStatusById(statusId: Long): UserStatus?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: UserStatus): Long

    @Delete
    suspend fun deleteStatus(status: UserStatus)

    @Query("DELETE FROM user_statuses WHERE id = :statusId")
    suspend fun deleteStatusById(statusId: Long)

    // Status Views
    @Query("SELECT * FROM status_views WHERE statusId = :statusId")
    fun getStatusViewsFlow(statusId: Long): Flow<List<StatusView>>

    @Query("SELECT * FROM status_views WHERE statusId = :statusId")
    suspend fun getStatusViews(statusId: Long): List<StatusView>

    @Query("SELECT COUNT(*) FROM status_views WHERE statusId = :statusId")
    suspend fun getStatusViewsCount(statusId: Long): Int

    @Query("SELECT * FROM status_views WHERE statusId = :statusId AND viewerUserId = :viewerUserId LIMIT 1")
    suspend fun getViewByViewer(statusId: Long, viewerUserId: Long): StatusView?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatusView(view: StatusView): Long
}
