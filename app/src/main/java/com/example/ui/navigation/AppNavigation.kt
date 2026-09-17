package com.example.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.di.AppContainer
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.wallet.WalletScreen
import com.example.utils.AppViewModelFactory

@Composable
fun AppNavigation(appContainer: AppContainer) {
    val navController = rememberNavController()
    val viewModelFactory = AppViewModelFactory(
        appContainer.authRepository,
        appContainer.userRepository,
        appContainer.seedDatabaseUseCase
    )

    NavHost(navController = navController, startDestination = Routes.Splash) {
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
                }
            )
        }
        
        composable(Routes.Wallet) {
            WalletScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
