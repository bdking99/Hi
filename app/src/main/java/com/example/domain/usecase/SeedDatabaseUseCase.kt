package com.example.domain.usecase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.data.local.dao.CountryDao
import com.example.data.local.dao.SessionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CountryEntity
import com.example.data.local.entity.ProfileEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.repository.AuthRepository
import com.example.utils.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class SeedDatabaseUseCase(
    private val countryDao: CountryDao,
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        const val DEFAULT_USER_ID = "fb_uid_884920"
        const val DEFAULT_SESSION_TOKEN = "session_token_alex_884920"
    }

    suspend fun execute() = withContext(Dispatchers.IO) {
        if (countryDao.getCount() == 0) {
            val countries = listOf(
                CountryEntity(name = "Bangladesh", code = "BD", phoneCode = "+880", currency = "BDT", currencySymbol = "৳", flag = "🇧🇩"),
                CountryEntity(name = "United States", code = "US", phoneCode = "+1", currency = "USD", currencySymbol = "$", flag = "🇺🇸"),
                CountryEntity(name = "United Kingdom", code = "GB", phoneCode = "+44", currency = "GBP", currencySymbol = "£", flag = "🇬🇧"),
                CountryEntity(name = "India", code = "IN", phoneCode = "+91", currency = "INR", currencySymbol = "₹", flag = "🇮🇳")
            )
            countryDao.insertCountries(countries)
        }

        // Check if default user exists
        val existingUser = userDao.getUser(DEFAULT_USER_ID)
        if (existingUser == null) {
            val defaultUser = UserEntity(
                id = DEFAULT_USER_ID,
                publicUserId = "884920",
                username = "alex_king",
                displayName = "Alex King 👑",
                email = "alex.king@greatvoiceroom.com",
                phone = "+1 (555) 884-9201",
                passwordHash = SecurityUtils.hashPassword("password123"),
                avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                coverImage = null,
                countryId = 1,
                roleId = 2,
                isVerified = true
            )
            val defaultProfile = ProfileEntity(
                userId = DEFAULT_USER_ID,
                bio = "👑 Top Host & Live Gamer | Global Voice Gala Winner ✨",
                level = 18,
                vipLevel = 3,
                svipLevel = 1,
                followersCount = 1420,
                followingCount = 380,
                agencyId = "Star_Talent_Agency",
                coinBalance = 158400L,
                earnings = 4820L
            )
            userDao.registerUser(defaultUser, defaultProfile)
        }

        // Auto-login persistence: Auto-login on app start unless explicit logout occurs
        val prefs = dataStore.data.firstOrNull()
        val wasExplicitlyLoggedOut = prefs?.get(AuthRepository.WAS_EXPLICITLY_LOGGED_OUT) ?: false
        val currentToken = prefs?.get(AuthRepository.SESSION_TOKEN)

        if (!wasExplicitlyLoggedOut) {
            val session = if (currentToken != null) sessionDao.getSession(currentToken) else null
            if (session == null) {
                val newSession = SessionEntity(
                    id = DEFAULT_SESSION_TOKEN,
                    userId = DEFAULT_USER_ID,
                    deviceId = "Android",
                    deviceName = "Primary Android Device",
                    expiresAt = System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000
                )
                sessionDao.insertSession(newSession)
                dataStore.edit {
                    it[AuthRepository.SESSION_TOKEN] = DEFAULT_SESSION_TOKEN
                    it[AuthRepository.WAS_EXPLICITLY_LOGGED_OUT] = false
                }
            }
        }
    }
}
