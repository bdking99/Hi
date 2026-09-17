package com.example.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.data.local.dao.SessionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.ProfileEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.UserEntity
import com.example.utils.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class AuthRepository(
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val SESSION_TOKEN = stringPreferencesKey("session_token")
    }

    val currentSessionToken: Flow<String?> = dataStore.data.map { it[SESSION_TOKEN] }

    suspend fun register(username: String, email: String, displayName: String, passwordRaw: String, countryId: Int?): Result<UserEntity> {
        return try {
            val existing = userDao.getUserByEmailOrPhone(email)
            if (existing != null) {
                return Result.failure(Exception("Email already in use"))
            }
            val existingUsername = userDao.getUserByUsername(username)
            if (existingUsername != null) {
                return Result.failure(Exception("Username already in use"))
            }

            val userId = UUID.randomUUID().toString()
            val publicUserId = (100000..999999).random().toString()
            val passwordHash = SecurityUtils.hashPassword(passwordRaw)

            val user = UserEntity(
                id = userId,
                publicUserId = publicUserId,
                username = username,
                displayName = displayName,
                email = email,
                phone = null,
                passwordHash = passwordHash,
                avatar = null,
                coverImage = null,
                countryId = countryId
            )
            val profile = ProfileEntity(
                userId = userId,
                bio = "Hello! I am new here.",
                agencyId = null
            )

            userDao.registerUser(user, profile)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, passwordRaw: String): Result<Pair<UserEntity, String>> {
        return try {
            val user = userDao.getUserByEmailOrPhone(email)
                ?: return Result.failure(Exception("User not found"))

            if (user.passwordHash != SecurityUtils.hashPassword(passwordRaw)) {
                return Result.failure(Exception("Invalid password"))
            }

            val sessionToken = UUID.randomUUID().toString()
            val session = SessionEntity(
                id = sessionToken,
                userId = user.id,
                deviceId = "Android",
                deviceName = "Mobile Device",
                expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 // 30 days
            )
            sessionDao.insertSession(session)
            
            dataStore.edit { it[SESSION_TOKEN] = sessionToken }

            Result.success(Pair(user, sessionToken))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        dataStore.edit {
            val token = it[SESSION_TOKEN]
            if (token != null) {
                sessionDao.deleteSession(token)
            }
            it.remove(SESSION_TOKEN)
        }
    }

    suspend fun getCurrentSession(token: String): SessionEntity? {
        return sessionDao.getSession(token)
    }
}
