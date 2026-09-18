package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.di.AppContainer
import com.example.ui.screens.game.GameScreen
import com.example.ui.screens.message.MessageScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.room.RoomScreen
import com.example.ui.screens.room.RoomViewModel
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import com.example.utils.AppViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun MainNavigation(
    appContainer: AppContainer, 
    onLogout: () -> Unit,
    onNavigateWallet: () -> Unit
) {
    val navController = rememberNavController()
    val viewModelFactory = AppViewModelFactory(
        appContainer.authRepository,
        appContainer.userRepository,
        appContainer.seedDatabaseUseCase,
        appContainer.roomRepository
    )
    val profileViewModel: ProfileViewModel = viewModel(factory = viewModelFactory)
    val roomViewModel: RoomViewModel = viewModel(factory = viewModelFactory)
    val profile by profileViewModel.profile.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val items = listOf(
        Pair(Tabs.Rooms, Icons.Filled.Podcasts to "Room"),
        Pair(Tabs.Games, Icons.Filled.SportsEsports to "Game"),
        Pair(Tabs.Messages, Icons.Filled.ChatBubble to "Message"),
        Pair(Tabs.Profile, Icons.Filled.Person to "Me")
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
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
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tabs.Rooms,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Room (Live Broadcasting & Social Feed)
            composable(Tabs.Rooms) {
                RoomScreen(
                    viewModel = roomViewModel,
                    onNavigateWallet = onNavigateWallet
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

            // 3. Message (Chat & Notifications)
            composable(Tabs.Messages) {
                MessageScreen()
            }

            // 4. Me (User Profile & Settings)
            composable(Tabs.Profile) { 
                ProfileScreen(
                    viewModel = profileViewModel,
                    onLogoutClick = onLogout,
                    onWalletClick = onNavigateWallet
                ) 
            }
        }
    }
}
