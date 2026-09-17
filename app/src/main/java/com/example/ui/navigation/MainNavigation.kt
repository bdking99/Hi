package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.di.AppContainer
import com.example.ui.screens.home.*
import com.example.ui.screens.profile.ProfileScreen
import com.example.utils.AppViewModelFactory

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
        appContainer.seedDatabaseUseCase
    )
    
    val items = listOf(
        Pair(Tabs.Rooms, Icons.Filled.List to "Room"),
        Pair(Tabs.Games, Icons.Filled.PlayArrow to "Game"),
        Pair(Tabs.Messages, Icons.Filled.MailOutline to "Message"),
        Pair(Tabs.Profile, Icons.Filled.Person to "Me")
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                items.forEach { (route, iconAndLabel) ->
                    val (icon, label) = iconAndLabel
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
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
            composable(Tabs.Rooms) { RoomsScreen() }
            composable(Tabs.Games) { GamesScreen() }
            composable(Tabs.Messages) { MessagesScreen() }
            composable(Tabs.Profile) { 
                ProfileScreen(
                    viewModel = viewModel(factory = viewModelFactory),
                    onLogoutClick = onLogout,
                    onWalletClick = onNavigateWallet
                ) 
            }
        }
    }
}
