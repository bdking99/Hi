package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.entity.*
import com.example.data.local.dao.*

@Database(
    entities = [
        UserEntity::class,
        ProfileEntity::class,
        RoleEntity::class,
        SessionEntity::class,
        CountryEntity::class,
        NotificationEntity::class,
        AuditLogEntity::class,
        SystemSettingEntity::class,
        WalletEntity::class,
        CoinTransactionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun countryDao(): CountryDao
}
