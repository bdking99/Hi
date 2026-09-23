package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.data.model.FirebaseUserProfile
import com.example.di.AppContainer
import com.example.ui.screens.call.CallViewModel
import com.example.ui.screens.call.VoiceCallOverlay
import com.example.ui.screens.game.GameScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.message.ChatViewModel
import com.example.ui.screens.message.MessageScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.room.FullScreenVoiceRoom
import com.example.ui.screens.room.RoomViewModel
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import com.example.utils.AppViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun MainNavigation(
    appContainer: AppContainer, 
    onLogout: () -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateVip: () -> Unit = {},
    onNavigateNotifications: () -> Unit = {},
    onNavigateModeration: () -> Unit = {},
    onNavigateAdmin: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val viewModelFactory = AppViewModelFactory(
        authRepository = appContainer.authRepository,
        userRepository = appContainer.userRepository,
        seedDatabaseUseCase = appContainer.seedDatabaseUseCase,
        roomRepository = appContainer.roomRepository,
        chatRepository = appContainer.chatRepository,
        callRepository = appContainer.callRepository,
        context = appContainer.appContext,
        walletRepository = appContainer.walletRepository,
        vipRepository = appContainer.vipRepository,
        notificationRepository = appContainer.notificationRepository,
        socialRepository = appContainer.socialRepository,
        moderationRepository = appContainer.moderationRepository,
        adminRepository = appContainer.adminRepository
    )
    val profileViewModel: ProfileViewModel = viewModel(factory = viewModelFactory)
    val roomViewModel: RoomViewModel = viewModel(factory = viewModelFactory)
    val chatViewModel: ChatViewModel = viewModel(factory = viewModelFactory)
    val callViewModel: CallViewModel = viewModel(factory = viewModelFactory)

    val profile by profileViewModel.profile.collectAsStateWithLifecycle()
    val currentRoom by roomViewModel.currentRoom.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var activeFullScreenRoomId by remember { mutableStateOf<String?>(null) }

    val items = listOf(
        Pair(Tabs.Rooms, Icons.Filled.Podcasts to "Home"),
        Pair(Tabs.Games, Icons.Filled.SportsEsports to "Games"),
        Pair(Tabs.Messages, Icons.Filled.ChatBubble to "Messages"),
        Pair(Tabs.Profile, Icons.Filled.Person to "Profile")
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0B1A))) {
        Scaffold(
            containerColor = Color(0xFF0F0B1A),
            bottomBar = {
                // Bottom bar is hidden when user is inside an active full screen voice room
                if (activeFullScreenRoomId == null) {
                    NavigationBar(
                        containerColor = Color(0xFF150F26),
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        
                        items.forEach { (route, iconAndLabel) ->
                            val (icon, label) = iconAndLabel
                            val isSelected = currentDestination?.hierarchy?.any { it.route == route } == true
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                selected = isSelected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = GoldPremium,
                                    indicatorColor = GoldPremium,
                                    unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.5f)
                                ),
                                onClick = {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Tabs.Rooms,
                modifier = Modifier.padding(innerPadding)
            ) {
                // 1. Home / Discover Voice Rooms Screen
                composable(Tabs.Rooms) {
                    HomeScreen(
                        viewModel = roomViewModel,
                        onSelectRoom = { roomId ->
                            activeFullScreenRoomId = roomId
                        },
                        onNavigateWallet = onNavigateWallet,
                        onNavigateGames = {
                            navController.navigate(Tabs.Games) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateNotifications = onNavigateNotifications
                    )
                }

                // 2. Game (Mini Games & Casino)
                composable(Tabs.Games) {
                    GameScreen(
                        currentCoins = profile?.coinBalance ?: 158400L,
                        vipLevel = profile?.vipLevel ?: 3,
                        onAddCoinsClick = onNavigateWallet,
                        onCoinWon = { wonAmount ->
                            coroutineScope.launch {
                                profile?.userId?.let { uid ->
                                    appContainer.userRepository.addCoins(uid, wonAmount)
                                }
                            }
                        }
                    )
                }

                // 3. Message (Real-Time Firestore Chat & Voice Call History)
                composable(Tabs.Messages) {
                    MessageScreen(
                        chatViewModel = chatViewModel,
                        callViewModel = callViewModel,
                        userRepository = appContainer.userRepository,
                        currentUserId = profile?.userId ?: ""
                    )
                }

                // 4. Me (User Profile & Settings)
                composable(Tabs.Profile) { 
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onLogoutClick = onLogout,
                        onWalletClick = onNavigateWallet,
                        onVipClick = onNavigateVip,
                        onNavigateNotifications = onNavigateNotifications,
                        onNavigateModeration = onNavigateModeration,
                        onNavigateAdmin = onNavigateAdmin,
                        onSendMessage = { targetUser ->
                            chatViewModel.openConversationWith(targetUser)
                            navController.navigate(Tabs.Messages) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onVoiceCall = { targetUser ->
                            callViewModel.initiateCall(targetUser)
                        }
                    ) 
                }
            }
        }

        // FULL SCREEN VOICE ROOM OVERLAY
        activeFullScreenRoomId?.let { roomId ->
            FullScreenVoiceRoom(
                viewModel = roomViewModel,
                roomId = roomId,
                onLeaveRoom = {
                    roomViewModel.closeCurrentRoom()
                    activeFullScreenRoomId = null
                },
                onNavigateWallet = onNavigateWallet,
                onNavigateDirectChat = { uid, name, avatar ->
                    chatViewModel.openConversationWith(
                        FirebaseUserProfile(
                            userId = uid,
                            displayName = name,
                            avatar = avatar
                        )
                    )
                    activeFullScreenRoomId = null
                    navController.navigate(Tabs.Messages)
                },
                onNavigateDirectCall = { uid, name, avatar ->
                    callViewModel.initiateCall(
                        FirebaseUserProfile(
                            userId = uid,
                            displayName = name,
                            avatar = avatar
                        )
                    )
                }
            )
        }

        // Global Voice Call Interface (Handles Outgoing, Incoming Ringing, and Active HD Voice Call)
        VoiceCallOverlay(viewModel = callViewModel)
    }
}
