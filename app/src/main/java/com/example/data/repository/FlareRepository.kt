package com.example.data.repository

import android.content.Context
import com.example.data.local.FlareDatabase
import com.example.data.model.*
import com.example.data.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserWithFriendship(
    val user: User,
    val friendshipStatus: String, // "ACCEPTED", "PENDING_OUTGOING", "PENDING_INCOMING", "BLOCKED"
    val streak: Streak?
)

data class StreakWithUser(
    val streak: Streak,
    val friend: User,
    val hoursRemainingToday: Int,
    val milestone: StreakMilestone?
)

data class ConversationWithFriend(
    val conversation: Conversation,
    val friend: User,
    val streak: Streak?
)

data class OwnerMetrics(
    val totalUsers: Int,
    val totalActiveStreaks: Int,
    val totalMessages: Int,
    val totalConversations: Int
)

class FlareRepository(private val context: Context) {
    private val db = FlareDatabase.getDatabase(context)
    private val userDao = db.userDao()
    private val friendshipDao = db.friendshipDao()
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val streakDao = db.streakDao()
    private val notificationDao = db.notificationDao()
    private val adminDao = db.adminDao()
    private val achievementDao = db.achievementDao()
    private val statusDao = db.statusDao()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            FlareDatabase.seedDatabase(db)
        }
    }

    // --- Authentication ---
    suspend fun login(username: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        val user = userDao.getUserByUsername(username.trim())
            ?: return@withContext Result.failure(Exception("Username tidak ditemukan."))

        if (user.isSuspended) {
            return@withContext Result.failure(Exception("Akun ini telah dinonaktifkan oleh Administrator."))
        }

        if (!SecurityUtils.verifyPassword(password, user.passwordHash)) {
            return@withContext Result.failure(Exception("Password yang Anda masukkan salah."))
        }

        userDao.updateOnlineStatus(user.id, true, System.currentTimeMillis())
        val onlineUser = user.copy(isOnline = true, lastSeen = System.currentTimeMillis())
        _currentUser.value = onlineUser
        Result.success(onlineUser)
    }

    suspend fun register(
        username: String,
        password: String,
        displayName: String,
        bio: String,
        avatarIndex: Int,
        customPhotoUri: String? = null
    ): Result<User> = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim()
        if (cleanUsername.length < 3) {
            return@withContext Result.failure(Exception("Username minimal 3 karakter."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(Exception("Password minimal 6 karakter."))
        }

        val existing = userDao.getUserByUsername(cleanUsername)
        if (existing != null) {
            return@withContext Result.failure(Exception("Username sudah digunakan. Silakan pilih username lain."))
        }

        // Generate guaranteed unique 6-digit ID
        var sixDigitId = SecurityUtils.generateSixDigitId()
        while (userDao.getUserBySixDigitId(sixDigitId) != null) {
            sixDigitId = SecurityUtils.generateSixDigitId()
        }

        val newUser = User(
            username = cleanUsername,
            passwordHash = SecurityUtils.hashPassword(password),
            displayName = displayName.ifBlank { cleanUsername },
            sixDigitId = sixDigitId,
            avatarIndex = avatarIndex,
            customPhotoUri = customPhotoUri,
            bio = bio,
            role = "USER",
            isVerifiedOwner = false,
            isOnline = true,
            lastSeen = System.currentTimeMillis()
        )

        val newId = userDao.insertUser(newUser)
        val created = newUser.copy(id = newId)

        if (!customPhotoUri.isNullOrBlank() || bio.isNotBlank()) {
            unlockAchievement(newId, FlareAchievements.PROFILE_CUSTOMIZER.key)
        }

        // Welcome notification
        notificationDao.insertNotification(
            NotificationItem(
                userId = newId,
                title = "Selamat Datang di FLARE! 🔥",
                body = "ID 6 Digit unik kamu adalah $sixDigitId. Bagikan ID ini kepada temanmu untuk mulai menyalakan api streak!",
                type = "MESSAGE",
                timestamp = System.currentTimeMillis()
            )
        )

        _currentUser.value = created
        Result.success(created)
    }

    fun switchAccount(user: User) {
        val old = _currentUser.value
        CoroutineScope(Dispatchers.IO).launch {
            if (old != null && old.id != user.id) {
                userDao.updateOnlineStatus(old.id, false, System.currentTimeMillis())
            }
            userDao.updateOnlineStatus(user.id, true, System.currentTimeMillis())
        }
        _currentUser.value = user.copy(isOnline = true)
    }

    fun logout() {
        val current = _currentUser.value
        if (current != null) {
            CoroutineScope(Dispatchers.IO).launch {
                userDao.updateOnlineStatus(current.id, false, System.currentTimeMillis())
            }
        }
        _currentUser.value = null
    }

    suspend fun toggleMyOnlineStatus(newStatus: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        val now = System.currentTimeMillis()
        userDao.updateOnlineStatus(current.id, newStatus, now)
        _currentUser.value = current.copy(isOnline = newStatus, lastSeen = now)
        Result.success(Unit)
    }

    companion object {
        fun formatLastSeen(isOnline: Boolean, lastSeen: Long): String {
            if (isOnline) return "Online"
            val diffMs = System.currentTimeMillis() - lastSeen
            val mins = (diffMs / (1000 * 60)).toInt()
            return when {
                mins < 1 -> "Offline • Baru saja"
                mins < 60 -> "Offline • $mins mnt lalu"
                mins < 1440 -> "Offline • ${mins / 60} jam lalu"
                else -> {
                    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                    "Offline • ${sdf.format(Date(lastSeen))}"
                }
            }
        }
    }

    suspend fun changePassword(oldPass: String, newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        if (!SecurityUtils.verifyPassword(oldPass, current.passwordHash)) {
            return@withContext Result.failure(Exception("Password lama tidak sesuai"))
        }
        if (newPass.length < 6) {
            return@withContext Result.failure(Exception("Password baru minimal 6 karakter"))
        }

        val updated = current.copy(
            passwordHash = SecurityUtils.hashPassword(newPass),
            mustChangePassword = false
        )
        userDao.updateUser(updated)
        _currentUser.value = updated
        Result.success(Unit)
    }

    suspend fun updateProfile(displayName: String, bio: String, avatarIndex: Int, customPhotoUri: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        val updated = current.copy(
            displayName = displayName.ifBlank { current.displayName },
            bio = bio,
            avatarIndex = avatarIndex,
            customPhotoUri = customPhotoUri ?: current.customPhotoUri
        )
        userDao.updateUser(updated)
        _currentUser.value = updated

        // Check Profile Customizer achievement
        unlockAchievement(current.id, FlareAchievements.PROFILE_CUSTOMIZER.key)

        Result.success(Unit)
    }

    // --- Achievements System ---
    fun getUserAchievementsFlow(userId: Long): Flow<List<UserAchievement>> {
        return achievementDao.getUserAchievementsFlow(userId)
    }

    suspend fun unlockAchievement(userId: Long, achievementKey: String) = withContext(Dispatchers.IO) {
        val existing = achievementDao.getAchievement(userId, achievementKey)
        if (existing == null) {
            achievementDao.insertAchievement(UserAchievement(userId = userId, achievementKey = achievementKey))
            val def = FlareAchievements.getByKey(achievementKey)
            if (def != null) {
                notificationDao.insertNotification(
                    NotificationItem(
                        userId = userId,
                        title = "Prestasi Terbuka: ${def.title}! ${def.iconEmoji}",
                        body = def.description,
                        type = "ACHIEVEMENT"
                    )
                )
            }
        }
    }

    // --- Flame Streak Invitation Rules ---
    // Rule: Pengguna tidak bisa membuat streak dengan owner kecuali owner mengirim pesan ajakan ke pengguna
    suspend fun canUserInviteFriendToFlame(friendId: Long): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Pair(false, "Sesi habis")
        val friend = userDao.getUserById(friendId) ?: return@withContext Pair(false, "Pengguna tidak ditemukan")

        if (friend.role == "OWNER" || friend.isVerifiedOwner) {
            if (current.role != "OWNER" && !current.isVerifiedOwner) {
                return@withContext Pair(
                    false,
                    "Pengguna biasa tidak bisa membuat streak dengan Owner kecuali Owner yang mengirimkan pesan ajakan api terlebih dahulu."
                )
            }
        }
        Pair(true, null)
    }

    suspend fun getStreakBetween(friendId: Long): Streak? = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext null
        streakDao.getStreakBetween(current.id, friendId)
    }

    // Send "Mau bikin api gak? 🔥" invitation
    suspend fun sendFlameInvite(friendId: Long): Result<Streak> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        val (canInvite, errorMsg) = canUserInviteFriendToFlame(friendId)
        if (!canInvite) {
            return@withContext Result.failure(Exception(errorMsg ?: "Tidak dapat mengajak"))
        }

        var streak = streakDao.getStreakBetween(current.id, friendId)
        if (streak == null) {
            val newStreak = Streak(
                user1Id = current.id,
                user2Id = friendId,
                currentStreak = 0,
                lastInteractionDate = "",
                inviteStatus = "PENDING_INVITE",
                invitedByUserId = current.id
            )
            val id = streakDao.insertStreak(newStreak)
            streak = newStreak.copy(id = id)
        } else {
            streak = streak.copy(
                inviteStatus = "PENDING_INVITE",
                invitedByUserId = current.id
            )
            streakDao.updateStreak(streak)
        }

        // Notify friend
        notificationDao.insertNotification(
            NotificationItem(
                userId = friendId,
                title = "Ajakan Api Baru! 🔥",
                body = "${current.displayName} mengajakmu: 'Mau bikin api gak?'",
                type = "FLAME_INVITE",
                relatedUserId = current.id
            )
        )

        unlockAchievement(current.id, FlareAchievements.STREAK_STARTER.key)

        Result.success(streak)
    }

    // Respond to Flame Invite: "Ya" -> open chat & activate flame; "Tidak" -> delete conversation automatically!
    suspend fun respondFlameInvite(friendId: Long, accept: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        val streak = streakDao.getStreakBetween(current.id, friendId)

        if (accept) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val updated = (streak ?: Streak(user1Id = friendId, user2Id = current.id)).copy(
                currentStreak = maxOf(1, streak?.currentStreak ?: 1),
                highestStreak = maxOf(1, streak?.highestStreak ?: 1),
                lastInteractionDate = todayStr,
                inviteStatus = "ACTIVE",
                user1SentToday = true,
                user2SentToday = true
            )
            if (streak == null) {
                streakDao.insertStreak(updated)
            } else {
                streakDao.updateStreak(updated)
            }

            // Create conversation if not exists
            getOrCreateConversation(friendId)

            notificationDao.insertNotification(
                NotificationItem(
                    userId = friendId,
                    title = "Api Resmi Menyala! 🔥",
                    body = "${current.displayName} menyetujui ajakan api! Chat kini dapat dibuka.",
                    type = "FLAME_INVITE",
                    relatedUserId = current.id
                )
            )

            unlockAchievement(current.id, FlareAchievements.STREAK_STARTER.key)
            unlockAchievement(friendId, FlareAchievements.STREAK_STARTER.key)
        } else {
            // "tidak, chat otomatis dihapus"
            if (streak != null) {
                streakDao.deleteStreakBetween(current.id, friendId)
            }
            conversationDao.deleteConversationBetween(current.id, friendId)
        }
        Result.success(Unit)
    }

    // --- Friends & 6-Digit Search ---
    fun getAllUsersFlow(): Flow<List<User>> = userDao.getAllUsersFlow()

    suspend fun searchUserBySixDigitId(sixDigitId: String): User? = withContext(Dispatchers.IO) {
        val user = userDao.getUserBySixDigitId(sixDigitId.trim())
        if (user != null) {
            _currentUser.value?.let { me ->
                unlockAchievement(me.id, FlareAchievements.FRIEND_FINDER.key)
            }
        }
        user
    }

    fun getFriendsForCurrentUser(): Flow<List<UserWithFriendship>> {
        return currentUser.flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(emptyList())
            combine(
                friendshipDao.getFriendshipsFlow(user.id),
                userDao.getAllUsersFlow(),
                streakDao.getStreaksFlow(user.id)
            ) { friendships, allUsers, streaks ->
                val userMap = allUsers.associateBy { it.id }
                friendships.mapNotNull { f ->
                    val otherId = if (f.userId == user.id) f.friendId else f.userId
                    val otherUser = userMap[otherId] ?: return@mapNotNull null
                    val status = if (f.status == "PENDING_INCOMING" || f.status == "PENDING_OUTGOING") {
                        if (f.userId == user.id) "PENDING_OUTGOING" else "PENDING_INCOMING"
                    } else {
                        f.status
                    }
                    val streak = streaks.find {
                        (it.user1Id == user.id && it.user2Id == otherId) ||
                        (it.user1Id == otherId && it.user2Id == user.id)
                    }
                    UserWithFriendship(otherUser, status, streak)
                }
            }
        }
    }

    suspend fun sendFriendRequest(targetUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        if (current.id == targetUserId) {
            return@withContext Result.failure(Exception("Tidak dapat menambahkan diri sendiri."))
        }
        val existing = friendshipDao.getFriendship(current.id, targetUserId)
        if (existing != null) {
            return@withContext Result.failure(Exception("Permintaan pertemanan sudah ada."))
        }

        friendshipDao.insertFriendship(
            Friendship(userId = current.id, friendId = targetUserId, status = "PENDING_OUTGOING")
        )

        notificationDao.insertNotification(
            NotificationItem(
                userId = targetUserId,
                title = "Permintaan Pertemanan Baru",
                body = "${current.displayName} (ID: ${current.sixDigitId}) ingin berteman denganmu!",
                type = "FRIEND_REQUEST",
                relatedUserId = current.id
            )
        )

        unlockAchievement(current.id, FlareAchievements.FRIEND_FINDER.key)

        Result.success(Unit)
    }

    suspend fun respondFriendRequest(otherUserId: Long, accept: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        val existing = friendshipDao.getFriendship(current.id, otherUserId)
        if (existing != null) {
            if (accept) {
                friendshipDao.updateUserFriendship(existing.copy(status = "ACCEPTED"))
                notificationDao.insertNotification(
                    NotificationItem(
                        userId = otherUserId,
                        title = "Pertemanan Diterima! 🤝",
                        body = "${current.displayName} menerima pertemananmu. Ajak mereka membuat api bersama!",
                        type = "FRIEND_ACCEPTED",
                        relatedUserId = current.id
                    )
                )
                unlockAchievement(current.id, FlareAchievements.FRIEND_FINDER.key)
            } else {
                friendshipDao.deleteFriendship(current.id, otherUserId)
            }
        }
        Result.success(Unit)
    }

    suspend fun removeFriend(otherUserId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        friendshipDao.deleteFriendship(current.id, otherUserId)
        streakDao.deleteStreakBetween(current.id, otherUserId)
        val conv = conversationDao.getConversationBetween(current.id, otherUserId)
        if (conv != null) {
            messageDao.deleteMessagesForConversation(conv.id)
            conversationDao.deleteConversation(conv.id)
        }
        Result.success(Unit)
    }

    suspend fun deleteConversation(conversationId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        messageDao.deleteMessagesForConversation(conversationId)
        conversationDao.deleteConversation(conversationId)
        Result.success(Unit)
    }

    suspend fun clearMessagesForConversation(conversationId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        messageDao.deleteMessagesForConversation(conversationId)
        val conv = conversationDao.getConversationById(conversationId)
        if (conv != null) {
            conversationDao.updateConversation(conv.copy(lastMessageText = null, lastMessageTime = System.currentTimeMillis()))
        }
        Result.success(Unit)
    }

    // --- Streak Logic (Unlimited Days) ---
    fun getStreaksFlow(): Flow<List<StreakWithUser>> {
        return currentUser.flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(emptyList())
            combine(
                streakDao.getStreaksFlow(user.id),
                userDao.getAllUsersFlow()
            ) { streaks, allUsers ->
                val userMap = allUsers.associateBy { it.id }
                val hoursRemaining = calculateHoursRemainingToday()
                streaks.filter { it.inviteStatus == "ACTIVE" }.mapNotNull { s ->
                    val otherId = if (s.user1Id == user.id) s.user2Id else s.user1Id
                    val friend = userMap[otherId] ?: return@mapNotNull null
                    val milestone = StreakMilestones.getMilestoneForDays(s.currentStreak)
                    StreakWithUser(s, friend, hoursRemaining, milestone)
                }.sortedByDescending { it.streak.currentStreak }
            }
        }
    }

    private fun calculateHoursRemainingToday(): Int {
        val now = java.util.Calendar.getInstance()
        val endOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }
        val diffMs = endOfDay.timeInMillis - now.timeInMillis
        return (diffMs / (1000 * 60 * 60)).toInt().coerceIn(1, 24)
    }

    // --- Conversations & Messaging ---
    fun getConversationsFlow(): Flow<List<ConversationWithFriend>> {
        return currentUser.flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(emptyList())
            combine(
                conversationDao.getConversationsFlow(user.id),
                userDao.getAllUsersFlow(),
                streakDao.getStreaksFlow(user.id)
            ) { convs, allUsers, streaks ->
                val userMap = allUsers.associateBy { it.id }
                convs.mapNotNull { conv ->
                    val otherId = if (conv.user1Id == user.id) conv.user2Id else conv.user1Id
                    val friend = userMap[otherId] ?: return@mapNotNull null
                    val streak = streaks.find {
                        (it.user1Id == user.id && it.user2Id == otherId) ||
                        (it.user1Id == otherId && it.user2Id == user.id)
                    }
                    ConversationWithFriend(conv, friend, streak)
                }
            }
        }
    }

    fun getMessagesFlow(conversationId: Long): Flow<List<Message>> {
        return messageDao.getMessagesFlow(conversationId)
    }

    suspend fun getOrCreateConversation(otherUserId: Long): Conversation = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: throw Exception("Not logged in")
        var conv = conversationDao.getConversationBetween(current.id, otherUserId)
        if (conv == null) {
            val newId = conversationDao.insertConversation(
                Conversation(
                    user1Id = current.id,
                    user2Id = otherUserId,
                    lastMessageText = null,
                    lastMessageTime = System.currentTimeMillis()
                )
            )
            conv = conversationDao.getConversationById(newId)!!
        }
        conv
    }

    suspend fun sendMessage(
        receiverId: Long,
        content: String,
        messageType: String = "TEXT",
        mediaUrl: String? = null,
        mediaName: String? = null,
        mediaSize: String? = null,
        replyToId: Long? = null,
        replyToText: String? = null
    ): Result<Message> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        val conversation = getOrCreateConversation(receiverId)

        val message = Message(
            conversationId = conversation.id,
            senderId = current.id,
            receiverId = receiverId,
            content = content,
            messageType = messageType,
            mediaUrl = mediaUrl,
            mediaName = mediaName,
            mediaSize = mediaSize,
            replyToId = replyToId,
            replyToText = replyToText,
            status = "SENT",
            isEncrypted = true,
            timestamp = System.currentTimeMillis()
        )

        val msgId = messageDao.insertMessage(message)
        val insertedMsg = message.copy(id = msgId)

        // Update conversation last message
        val displaySnippet = when (messageType) {
            "IMAGE" -> "📷 Foto"
            "AUDIO" -> "🎤 Pesan Suara"
            "VIDEO" -> "🎥 Video"
            "FILE" -> "📁 $mediaName"
            else -> content
        }
        conversationDao.updateConversation(
            conversation.copy(
                lastMessageText = displaySnippet,
                lastMessageTime = System.currentTimeMillis()
            )
        )

        // Process Streak
        updateStreakForInteraction(current.id, receiverId)

        // Achievements triggers
        val count = messageDao.countMessagesBySender(current.id)
        if (count >= 1) {
            unlockAchievement(current.id, FlareAchievements.FIRST_MESSAGE.key)
        }
        if (count >= 10) {
            unlockAchievement(current.id, FlareAchievements.CHATTERBOX.key)
        }
        if (messageType != "TEXT") {
            unlockAchievement(current.id, FlareAchievements.MEDIA_PIONEER.key)
        }

        Result.success(insertedMsg)
    }

    private suspend fun updateStreakForInteraction(senderId: Long, receiverId: Long) {
        var streak = streakDao.getStreakBetween(senderId, receiverId)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (streak == null || streak.inviteStatus != "ACTIVE") return

        val isSenderUser1 = streak.user1Id == senderId
        val user1Sent = if (isSenderUser1) true else streak.user1SentToday
        val user2Sent = if (!isSenderUser1) true else streak.user2SentToday

        val updatedStreakCount = if (user1Sent && user2Sent && streak.lastInteractionDate != todayStr) {
            streak.currentStreak + 1
        } else if (streak.currentStreak == 0 && user1Sent && user2Sent) {
            1
        } else {
            streak.currentStreak
        }

        val updated = streak.copy(
            currentStreak = updatedStreakCount,
            highestStreak = maxOf(streak.highestStreak, updatedStreakCount),
            lastInteractionDate = todayStr,
            user1SentToday = user1Sent,
            user2SentToday = user2Sent
        )
        streakDao.updateStreak(updated)

        if (updatedStreakCount >= 7) {
            unlockAchievement(senderId, FlareAchievements.FLAME_KEEPER.key)
            unlockAchievement(receiverId, FlareAchievements.FLAME_KEEPER.key)
        }
    }

    suspend fun deleteMessage(messageId: Long) = withContext(Dispatchers.IO) {
        messageDao.deleteMessage(messageId)
    }

    // --- Notifications ---
    fun getNotificationsFlow(): Flow<List<NotificationItem>> {
        return currentUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else notificationDao.getNotificationsFlow(user.id)
        }
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext
        notificationDao.clearAll(current.id)
    }

    // --- Owner & Admin Dashboard ---
    suspend fun getOwnerMetrics(): OwnerMetrics = withContext(Dispatchers.IO) {
        val allUsers = userDao.getAllUsersFlow().first()
        val allStreaks = streakDao.getStreaksFlow(0).first()
        val allConvs = conversationDao.getConversationsFlow(0).first()

        OwnerMetrics(
            totalUsers = allUsers.size,
            totalActiveStreaks = allStreaks.count { it.currentStreak > 0 && it.inviteStatus == "ACTIVE" },
            totalMessages = 42,
            totalConversations = allConvs.size
        )
    }

    suspend fun toggleUserSuspension(targetUser: User): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        if (current.role != "OWNER") return@withContext Result.failure(Exception("Akses ditolak: Hanya Owner"))
        if (targetUser.role == "OWNER") return@withContext Result.failure(Exception("Tidak dapat menonaktifkan akun Owner"))

        val newSuspended = !targetUser.isSuspended
        userDao.updateUser(targetUser.copy(isSuspended = newSuspended))

        adminDao.insertAuditLog(
            AuditLog(
                adminUsername = current.username,
                action = if (newSuspended) "SUSPEND_USER" else "UNSUSPEND_USER",
                target = targetUser.username,
                details = "Status akun ${targetUser.username} (ID: ${targetUser.sixDigitId}) diubah ke: ${if (newSuspended) "SUSPENDED" else "ACTIVE"}"
            )
        )
        Result.success(Unit)
    }

    suspend fun toggleVerifiedOwnerBadge(targetUser: User): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        if (current.role != "OWNER") return@withContext Result.failure(Exception("Akses ditolak: Hanya Owner"))

        val newStatus = !targetUser.isVerifiedOwner
        userDao.updateUser(targetUser.copy(isVerifiedOwner = newStatus))

        adminDao.insertAuditLog(
            AuditLog(
                adminUsername = current.username,
                action = "TOGGLE_OWNER_BADGE",
                target = targetUser.username,
                details = "Centang Emas Owner ${targetUser.username} diubah menjadi: $newStatus"
            )
        )
        Result.success(Unit)
    }

    fun getAnnouncementsFlow(): Flow<List<Announcement>> = adminDao.getAnnouncementsFlow()
    fun getAuditLogsFlow(): Flow<List<AuditLog>> = adminDao.getAuditLogsFlow()

    suspend fun createAnnouncement(title: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        if (current.role != "OWNER") return@withContext Result.failure(Exception("Akses ditolak: Hanya Owner"))

        adminDao.insertAnnouncement(
            Announcement(
                title = title,
                content = content,
                author = "${current.displayName} (Owner)"
            )
        )
        Result.success(Unit)
    }

    // --- User Statuses (Stories) ---
    fun getActiveStatusesWithUsersFlow(): Flow<List<StatusWithUser>> {
        return currentUser.flatMapLatest { me ->
            combine(
                statusDao.getActiveStatusesFlow(),
                userDao.getAllUsersFlow()
            ) { statuses, allUsers ->
                val userMap = allUsers.associateBy { it.id }
                statuses.mapNotNull { st ->
                    val user = userMap[st.userId] ?: return@mapNotNull null
                    val views = statusDao.getStatusViews(st.id)
                    val isViewedByMe = if (me != null) views.any { it.viewerUserId == me.id } else false
                    val viewers = views.mapNotNull { v -> userMap[v.viewerUserId] }
                    StatusWithUser(
                        status = st,
                        user = user,
                        viewCount = views.size,
                        isViewedByMe = isViewedByMe,
                        viewers = viewers
                    )
                }
            }
        }
    }

    suspend fun createStatus(
        statusType: String,
        content: String,
        mediaUri: String? = null,
        backgroundColorHex: String = "#FF5E00"
    ): Result<UserStatus> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        val status = UserStatus(
            userId = current.id,
            statusType = statusType,
            content = content,
            mediaUri = mediaUri,
            backgroundColorHex = backgroundColorHex,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        )
        val id = statusDao.insertStatus(status)
        val created = status.copy(id = id)

        if (statusType != "TEXT") {
            unlockAchievement(current.id, FlareAchievements.MEDIA_PIONEER.key)
        }

        Result.success(created)
    }

    suspend fun recordStatusView(statusId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        val existing = statusDao.getViewByViewer(statusId, current.id)
        if (existing == null) {
            statusDao.insertStatusView(StatusView(statusId = statusId, viewerUserId = current.id))
        }
        Result.success(Unit)
    }

    suspend fun deleteStatus(statusId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        val status = statusDao.getStatusById(statusId) ?: return@withContext Result.failure(Exception("Status tidak ditemukan"))
        if (status.userId != current.id && current.role != "OWNER") {
            return@withContext Result.failure(Exception("Tidak memiliki izin menghapus status ini"))
        }
        statusDao.deleteStatusById(statusId)
        Result.success(Unit)
    }

    // Reply to status via direct message!
    suspend fun replyToStatus(
        status: UserStatus,
        statusAuthor: User,
        replyMessage: String
    ): Result<Message> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("Sesi habis"))
        val promptSnippet = when (status.statusType) {
            "IMAGE" -> "Membalas status foto: \"${status.content.ifBlank { "Foto" }}\""
            "VIDEO" -> "Membalas status video: \"${status.content.ifBlank { "Video" }}\""
            else -> "Membalas status: \"${status.content}\""
        }
        val fullContent = "$promptSnippet\n\n💬 $replyMessage"
        sendMessage(
            receiverId = statusAuthor.id,
            content = fullContent,
            messageType = "TEXT"
        )
    }
}
