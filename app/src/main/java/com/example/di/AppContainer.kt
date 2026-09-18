package com.example.di

import android.content.Context
import androidx.room.Room
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.AppDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.RoomRepository
import com.example.domain.usecase.SeedDatabaseUseCase

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppContainer(private val context: Context) {
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

    val userRepository: UserRepository by lazy {
        UserRepository(database.userDao())
    }

    val seedDatabaseUseCase: SeedDatabaseUseCase by lazy {
        SeedDatabaseUseCase(database.countryDao(), database.userDao(), database.sessionDao(), context.dataStore)
    }

    val roomRepository: RoomRepository by lazy {
        RoomRepository()
    }
}
