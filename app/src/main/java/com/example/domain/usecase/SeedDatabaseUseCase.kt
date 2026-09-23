package com.example.domain.usecase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.data.local.dao.CountryDao
import com.example.data.local.dao.SessionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CountryEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class SeedDatabaseUseCase(
    private val countryDao: CountryDao,
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val dataStore: DataStore<Preferences>
) {
    suspend fun execute() = withContext(Dispatchers.IO) {
        // 1. Seed countries reference table if empty
        if (countryDao.getCount() == 0) {
            val countries = listOf(
                CountryEntity(name = "Bangladesh", code = "BD", phoneCode = "+880", currency = "BDT", currencySymbol = "৳", flag = "🇧🇩"),
                CountryEntity(name = "United States", code = "US", phoneCode = "+1", currency = "USD", currencySymbol = "$", flag = "🇺🇸"),
                CountryEntity(name = "United Kingdom", code = "GB", phoneCode = "+44", currency = "GBP", currencySymbol = "£", flag = "🇬🇧"),
                CountryEntity(name = "India", code = "IN", phoneCode = "+91", currency = "INR", currencySymbol = "₹", flag = "🇮🇳"),
                CountryEntity(name = "United Arab Emirates", code = "AE", phoneCode = "+971", currency = "AED", currencySymbol = "د.إ", flag = "🇦🇪"),
                CountryEntity(name = "Saudi Arabia", code = "SA", phoneCode = "+966", currency = "SAR", currencySymbol = "﷼", flag = "🇸🇦"),
                CountryEntity(name = "Canada", code = "CA", phoneCode = "+1", currency = "CAD", currencySymbol = "$", flag = "🇨🇦")
            )
            countryDao.insertCountries(countries)
        }

        // 2. Synchronize real Firebase Authentication state with local session
        val currentFirebaseUser = FirebaseAuth.getInstance().currentUser
        val prefs = dataStore.data.firstOrNull()
        val wasExplicitlyLoggedOut = prefs?.get(AuthRepository.WAS_EXPLICITLY_LOGGED_OUT) ?: false

        if (currentFirebaseUser != null && !wasExplicitlyLoggedOut) {
            val currentToken = prefs?.get(AuthRepository.SESSION_TOKEN)
            val session = if (currentToken != null) sessionDao.getSession(currentToken) else null
            if (session == null) {
                val newToken = UUID.randomUUID().toString()
                val newSession = SessionEntity(
                    id = newToken,
                    userId = currentFirebaseUser.uid,
                    deviceId = "Android",
                    deviceName = "Mobile Device",
                    expiresAt = System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000
                )
                sessionDao.insertSession(newSession)
                dataStore.edit {
                    it[AuthRepository.SESSION_TOKEN] = newToken
                    it[AuthRepository.CURRENT_USER_ID] = currentFirebaseUser.uid
                    it[AuthRepository.WAS_EXPLICITLY_LOGGED_OUT] = false
                }
            }
        }
    }
}

