package com.example.utils

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.*
import com.example.domain.usecase.SeedDatabaseUseCase
import com.example.ui.screens.admin.AdminViewModel
import com.example.ui.screens.auth.AuthViewModel
import com.example.ui.screens.call.CallViewModel
import com.example.ui.screens.message.ChatViewModel
import com.example.ui.screens.moderation.ModerationViewModel
import com.example.ui.screens.notification.NotificationViewModel
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.room.RoomViewModel
import com.example.ui.screens.social.SocialViewModel
import com.example.ui.screens.splash.SplashViewModel
import com.example.ui.screens.vip.VipViewModel

class AppViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val seedDatabaseUseCase: SeedDatabaseUseCase,
    private val roomRepository: RoomRepository,
    private val chatRepository: ChatRepository,
    private val callRepository: CallRepository,
    private val context: Context,
    private val walletRepository: WalletRepository? = null,
    private val vipRepository: VipRepository? = null,
    private val notificationRepository: NotificationRepository? = null,
    private val socialRepository: SocialRepository? = null,
    private val moderationRepository: ModerationRepository? = null,
    private val adminRepository: AdminRepository? = null
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
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatRepository, authRepository, userRepository) as T
        }
        if (modelClass.isAssignableFrom(CallViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CallViewModel(context, callRepository, authRepository, userRepository) as T
        }
        if (modelClass.isAssignableFrom(VipViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VipViewModel(
                vipRepository ?: VipRepository(),
                authRepository,
                userRepository,
                walletRepository ?: WalletRepository()
            ) as T
        }
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(
                notificationRepository ?: NotificationRepository(),
                authRepository
            ) as T
        }
        if (modelClass.isAssignableFrom(SocialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SocialViewModel(
                socialRepository ?: SocialRepository(),
                authRepository,
                userRepository
            ) as T
        }
        if (modelClass.isAssignableFrom(ModerationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ModerationViewModel(
                moderationRepository ?: ModerationRepository(),
                authRepository,
                userRepository
            ) as T
        }
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(
                adminRepository = adminRepository ?: AdminRepository(),
                moderationRepository = moderationRepository ?: ModerationRepository(),
                authRepository = authRepository,
                userRepository = userRepository,
                walletRepository = walletRepository ?: WalletRepository()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
