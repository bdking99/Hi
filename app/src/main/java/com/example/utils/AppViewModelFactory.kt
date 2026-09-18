package com.example.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import com.example.domain.usecase.SeedDatabaseUseCase
import com.example.ui.screens.auth.AuthViewModel
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.splash.SplashViewModel

import com.example.data.repository.RoomRepository
import com.example.ui.screens.room.RoomViewModel

class AppViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val seedDatabaseUseCase: SeedDatabaseUseCase,
    private val roomRepository: RoomRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SplashViewModel(authRepository, seedDatabaseUseCase) as T
        }
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository) as T
        }
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(authRepository, userRepository) as T
        }
        if (modelClass.isAssignableFrom(RoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoomViewModel(roomRepository, authRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
