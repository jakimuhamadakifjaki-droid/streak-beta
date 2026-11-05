package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.repository.FlareRepository
import com.example.ui.components.FlareBottomNav
import com.example.ui.components.FlareTab
import com.example.ui.screens.*
import com.example.ui.theme.FlareDarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var repository: FlareRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = FlareRepository(applicationContext)

        setContent {
            MyApplicationTheme {
                FlareApp(repository = repository)
            }
        }
    }
}

@Composable
fun FlareApp(repository: FlareRepository) {
    val currentUser by repository.currentUser.collectAsState()
    val notifications by repository.getNotificationsFlow().collectAsState(initial = emptyList())
    val unreadNotifs = notifications.count { !it.isRead }

    var currentTab by remember { mutableStateOf(FlareTab.HOME) }

    // Subscreen navigation states
    var activeChatFriendId by remember { mutableStateOf<Long?>(null) }
    var showOwnerDashboard by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var viewingStatus by remember { mutableStateOf<com.example.data.model.StatusWithUser?>(null) }

    if (currentUser == null) {
        AuthScreen(
            repository = repository,
            onAuthSuccess = {
                currentTab = FlareTab.HOME
            }
        )
    } else {
        val user = currentUser!!

        if (viewingStatus != null) {
            StatusViewerScreen(
                statusItem = viewingStatus!!,
                currentUser = user,
                repository = repository,
                onClose = { viewingStatus = null },
                onNavigateToChat = { friendId ->
                    viewingStatus = null
                    activeChatFriendId = friendId
                }
            )
        } else if (showOwnerDashboard) {
            OwnerDashboardScreen(
                repository = repository,
                onBack = { showOwnerDashboard = false }
            )
        } else if (showNotifications) {
            NotificationScreen(
                repository = repository,
                onBack = { showNotifications = false }
            )
        } else if (activeChatFriendId != null) {
            ChatConversationScreen(
                friendId = activeChatFriendId!!,
                repository = repository,
                onBack = { activeChatFriendId = null }
            )
        } else {
            // Main Bottom Nav Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FlareDarkBackground)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_transition",
                    modifier = Modifier.fillMaxSize()
                ) { tab ->
                    when (tab) {
                        FlareTab.HOME -> {
                            HomeScreen(
                                currentUser = user,
                                repository = repository,
                                onNavigateToChat = { activeChatFriendId = it },
                                onNavigateToStreak = { currentTab = FlareTab.STREAK },
                                onNavigateToFriends = { currentTab = FlareTab.FRIENDS },
                                onNavigateToNotifications = { showNotifications = true },
                                onOpenStatus = { viewingStatus = it },
                                unreadNotifications = unreadNotifs
                            )
                        }
                        FlareTab.STREAK -> {
                            StreakScreen(
                                repository = repository,
                                onNavigateToChat = { activeChatFriendId = it }
                            )
                        }
                        FlareTab.CHAT -> {
                            ChatListScreen(
                                repository = repository,
                                onSelectConversation = { activeChatFriendId = it }
                            )
                        }
                        FlareTab.FRIENDS -> {
                            FriendsScreen(
                                repository = repository,
                                onNavigateToChat = { activeChatFriendId = it }
                            )
                        }
                        FlareTab.PROFILE -> {
                            ProfileScreen(
                                currentUser = user,
                                repository = repository,
                                onNavigateToOwnerDashboard = { showOwnerDashboard = true },
                                onLogout = { currentTab = FlareTab.HOME }
                            )
                        }
                    }
                }

                // Modern Floating Bottom Navigation
                FlareBottomNav(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    unreadChatCount = 0,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
