package com.example.di

import android.content.Context
import androidx.room.Room
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.AppDatabase
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.GameRepository
import com.example.data.repository.ModerationRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RoomRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.VipRepository
import com.example.data.repository.WalletRepository
import com.example.domain.usecase.SeedDatabaseUseCase

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppContainer(private val context: Context) {
    val appContext: Context get() = context

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "great_voice_room_db"
        ).fallbackToDestructiveMigration().build()
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(database.userDao(), database.sessionDao(), context.dataStore)
    }

    val notificationRepository: NotificationRepository by lazy {
        NotificationRepository()
    }

    val socialRepository: SocialRepository by lazy {
        SocialRepository(notificationRepository = notificationRepository)
    }

    val moderationRepository: ModerationRepository by lazy {
        ModerationRepository(notificationRepository = notificationRepository)
    }

    val adminRepository: AdminRepository by lazy {
        AdminRepository(notificationRepository = notificationRepository)
    }

    val userRepository: UserRepository by lazy {
        UserRepository(database.userDao())
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(userRepository = userRepository, socialRepository = socialRepository)
    }

    val callRepository: CallRepository by lazy {
        CallRepository(chatRepository = chatRepository, userRepository = userRepository)
    }

    val seedDatabaseUseCase: SeedDatabaseUseCase by lazy {
        SeedDatabaseUseCase(database.countryDao(), database.userDao(), database.sessionDao(), context.dataStore)
    }

    val roomRepository: RoomRepository by lazy {
        RoomRepository()
    }

    val walletRepository: WalletRepository by lazy {
        WalletRepository()
    }

    val gameRepository: GameRepository by lazy {
        GameRepository()
    }

    val vipRepository: VipRepository by lazy {
        VipRepository(context.dataStore)
    }
}
