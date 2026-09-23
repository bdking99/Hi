package com.example.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.di.AppContainer
import com.example.ui.screens.admin.AdminPanelScreen
import com.example.ui.screens.admin.AdminViewModel
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.launch.LaunchScreen
import com.example.ui.screens.moderation.ModerationViewModel
import com.example.ui.screens.moderation.ModeratorDashboardScreen
import com.example.ui.screens.notification.NotificationCenterScreen
import com.example.ui.screens.notification.NotificationSettingsScreen
import com.example.ui.screens.notification.NotificationViewModel
import com.example.ui.screens.social.SocialViewModel
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.vip.VipCenterScreen
import com.example.ui.screens.vip.VipViewModel
import com.example.ui.screens.wallet.WalletScreen
import com.example.utils.AppViewModelFactory

@Composable
fun AppNavigation(appContainer: AppContainer) {
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

    NavHost(navController = navController, startDestination = Routes.Launch) {
        // Launch Screen: Great Voice Room logo with smooth fade-in and scale animation
        composable(Routes.Launch) {
            LaunchScreen(
                onNavigateToMain = {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Launch) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Splash) {
            SplashScreen(
                viewModel = viewModel(factory = viewModelFactory),
                onNavigateNext = { hasSession ->
                    if (hasSession) {
                        navController.navigate(Routes.Main) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.Login) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(Routes.Login) {
            LoginScreen(
                viewModel = viewModel(factory = viewModelFactory),
                onLoginSuccess = {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
                onNavigateRegister = {
                    navController.navigate(Routes.Register)
                },
                onNavigateForgotPassword = {
                    // Navigate to Forgot Password
                }
            )
        }
        
        composable(Routes.Register) {
            RegisterScreen(
                viewModel = viewModel(factory = viewModelFactory),
                onRegisterSuccess = {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
                onNavigateLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Routes.Main) {
            MainNavigation(
                appContainer = appContainer,
                onLogout = {
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Main) { inclusive = true }
                    }
                },
                onNavigateWallet = {
                    navController.navigate(Routes.Wallet)
                },
                onNavigateVip = {
                    navController.navigate(Routes.VipCenter)
                },
                onNavigateNotifications = {
                    navController.navigate(Routes.Notifications)
                },
                onNavigateModeration = {
                    navController.navigate(Routes.ModerationDashboard)
                },
                onNavigateAdmin = {
                    navController.navigate(Routes.AdminPanel)
                }
            )
        }
        
        composable(Routes.Wallet) {
            WalletScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.VipCenter) {
            val vipViewModel: VipViewModel = viewModel(factory = viewModelFactory)
            VipCenterScreen(
                viewModel = vipViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWallet = { navController.navigate(Routes.Wallet) }
            )
        }

        composable(Routes.Notifications) {
            val notifViewModel: NotificationViewModel = viewModel(factory = viewModelFactory)
            NotificationCenterScreen(
                viewModel = notifViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.NotificationSettings) },
                onNavigateToProfile = { uid ->
                    navController.navigate(Routes.Main)
                },
                onNavigateToChat = { convId ->
                    navController.navigate(Routes.Main)
                },
                onNavigateToRoom = { roomId ->
                    navController.navigate(Routes.Main)
                },
                onNavigateToVip = {
                    navController.navigate(Routes.VipCenter)
                },
                onNavigateToGame = {
                    navController.navigate(Routes.Main)
                },
                onNavigateToReports = {
                    navController.navigate(Routes.ModerationDashboard)
                }
            )
        }

        composable(Routes.NotificationSettings) {
            val notifViewModel: NotificationViewModel = viewModel(factory = viewModelFactory)
            NotificationSettingsScreen(
                viewModel = notifViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ModerationDashboard) {
            val modViewModel: ModerationViewModel = viewModel(factory = viewModelFactory)
            ModeratorDashboardScreen(
                viewModel = modViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AdminPanel) {
            val adminViewModel: AdminViewModel = viewModel(factory = viewModelFactory)
            AdminPanelScreen(
                viewModel = adminViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
