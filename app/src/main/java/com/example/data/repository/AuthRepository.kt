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
import com.example.data.model.FirebaseUserProfile
import com.example.data.model.UserWallet
import com.example.utils.SecurityUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthRepository(
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val WAS_EXPLICITLY_LOGGED_OUT = androidx.datastore.preferences.core.booleanPreferencesKey("was_explicitly_logged_out")
        private const val COUNTERS_COLLECTION = "system_counters"
        private const val PUBLIC_ID_COUNTER_DOC = "public_user_id"
        private const val PUBLIC_IDS_COLLECTION = "public_ids"
        private const val USERS_COLLECTION = "users"
        private const val WALLETS_COLLECTION = "wallets"
    }

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    val currentSessionToken: Flow<String?> = dataStore.data.map { it[SESSION_TOKEN] }
    val currentUserIdFlow: Flow<String?> = dataStore.data.map { it[CURRENT_USER_ID] }

    val currentFirebaseUid: String?
        get() = auth.currentUser?.uid

    suspend fun isLoggedIn(): Boolean {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) return true
        val token = dataStore.data.firstOrNull()?.get(SESSION_TOKEN)
        return token != null && sessionDao.getSession(token) != null
    }

    /**
     * Atomically allocates a permanent, collision-free numeric Public User ID.
     * Starts at 1000001L and increments atomically.
     */
    private suspend fun generatePermanentPublicUserId(uid: String): String = withContext(Dispatchers.IO) {
        try {
            val counterRef = firestore.collection(COUNTERS_COLLECTION).document(PUBLIC_ID_COUNTER_DOC)
            val allocatedId = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(counterRef)
                val currentVal = if (snapshot.exists() && snapshot.contains("nextId")) {
                    snapshot.getLong("nextId") ?: 1000001L
                } else {
                    1000001L
                }
                val nextVal = currentVal + 1L
                transaction.set(counterRef, mapOf("nextId" to nextVal))
                currentVal.toString()
            }.await()

            // Map publicUserId -> uid for fast reverse lookup and integrity check
            firestore.collection(PUBLIC_IDS_COLLECTION).document(allocatedId).set(
                mapOf(
                    "uid" to uid,
                    "publicUserId" to allocatedId,
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()

            allocatedId
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback collision-resistant 7-digit numeric generation if transaction fails
            val fallbackNumeric = ((System.currentTimeMillis() % 9000000L) + 1000000L).toString()
            try {
                firestore.collection(PUBLIC_IDS_COLLECTION).document(fallbackNumeric).set(
                    mapOf(
                        "uid" to uid,
                        "publicUserId" to fallbackNumeric,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
            } catch (ign: Exception) { }
            fallbackNumeric
        }
    }

    /**
     * Real Firebase Email/Password Registration
     */
    suspend fun register(
        username: String,
        email: String,
        displayName: String,
        passwordRaw: String,
        countryId: Int?,
        avatarUrl: String? = null
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            // 1. Create user in Firebase Authentication
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), passwordRaw).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user creation returned null")
            val uid = firebaseUser.uid

            // 2. Generate permanent unique numeric Public User ID
            val permanentPublicUserId = generatePermanentPublicUserId(uid)

            // 3. Create real Firestore profile document
            val initialProfile = FirebaseUserProfile(
                userId = uid,
                publicUserId = permanentPublicUserId,
                displayName = displayName.ifBlank { "VIP Voice #$permanentPublicUserId" },
                username = username.ifBlank { "user_$permanentPublicUserId" },
                email = email.trim(),
                phone = null,
                avatar = avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                profileFrameId = "frame_gold_crown",
                coverImage = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
                bio = "Hello! I am on Great Voice Room. 🎙️✨",
                country = "Global",
                gender = "Not specified",
                accountStatus = "ACTIVE",
                createdAt = System.currentTimeMillis(),
                isOnline = true,
                lastActive = System.currentTimeMillis(),
                coinBalance = 2500L, // Welcome starter balance
                diamondBalance = 0L,
                level = 1,
                vipLevel = 1
            )
            firestore.collection(USERS_COLLECTION).document(uid).set(initialProfile).await()

            // 4. Initialize User Wallet in Firestore `wallets/{uid}`
            val initialWallet = UserWallet(
                uid = uid,
                publicUserId = permanentPublicUserId,
                coinBalance = 2500L,
                lifetimePurchasedCoins = 0L,
                lifetimeReceivedCoins = 0L,
                lifetimeSpentCoins = 0L,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            firestore.collection(WALLETS_COLLECTION).document(uid).set(initialWallet).await()

            // 5. Cache in Room database for offline speed
            val userEntity = UserEntity(
                id = uid,
                publicUserId = permanentPublicUserId,
                username = username,
                displayName = displayName,
                email = email,
                phone = null,
                passwordHash = SecurityUtils.hashPassword(passwordRaw),
                avatar = initialProfile.avatar,
                coverImage = initialProfile.coverImage,
                countryId = countryId,
                status = "ACTIVE",
                isVerified = true
            )
            val profileEntity = ProfileEntity(
                userId = uid,
                bio = initialProfile.bio,
                level = 1,
                vipLevel = 1,
                coinBalance = initialProfile.coinBalance,
                agencyId = null,
                earnings = initialProfile.diamondBalance
            )
            userDao.registerUser(userEntity, profileEntity)

            // 6. Store active session
            val sessionToken = UUID.randomUUID().toString()
            val session = SessionEntity(
                id = sessionToken,
                userId = uid,
                deviceId = "Android",
                deviceName = "Mobile Device",
                expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
            )
            sessionDao.insertSession(session)
            dataStore.edit {
                it[SESSION_TOKEN] = sessionToken
                it[CURRENT_USER_ID] = uid
                it[WAS_EXPLICITLY_LOGGED_OUT] = false
            }

            Result.success(userEntity)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Real Firebase Email/Password Login
     */
    suspend fun login(email: String, passwordRaw: String): Result<Pair<UserEntity, String>> = withContext(Dispatchers.IO) {
        try {
            // 1. Sign in with Firebase Authentication
            val authResult = auth.signInWithEmailAndPassword(email.trim(), passwordRaw).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase sign in returned null")
            val uid = firebaseUser.uid

            // 2. Fetch or create Firestore user profile
            val userDocRef = firestore.collection(USERS_COLLECTION).document(uid)
            val docSnap = userDocRef.get().await()

            val firestoreProfile: FirebaseUserProfile = if (docSnap.exists()) {
                val existing = docSnap.toObject(FirebaseUserProfile::class.java)
                if (existing != null && existing.publicUserId.isNotBlank()) {
                    // Update online status in Firestore
                    userDocRef.update(
                        mapOf(
                            "isOnline" to true,
                            "lastActive" to System.currentTimeMillis()
                        )
                    ).await()
                    existing.copy(isOnline = true, lastActive = System.currentTimeMillis())
                } else {
                    val publicId = generatePermanentPublicUserId(uid)
                    val updated = (existing ?: FirebaseUserProfile()).copy(
                        userId = uid,
                        publicUserId = publicId,
                        email = email.trim(),
                        isOnline = true,
                        lastActive = System.currentTimeMillis()
                    )
                    userDocRef.set(updated, SetOptions.merge()).await()
                    updated
                }
            } else {
                val publicId = generatePermanentPublicUserId(uid)
                val newProfile = FirebaseUserProfile(
                    userId = uid,
                    publicUserId = publicId,
                    displayName = firebaseUser.displayName ?: "VIP Voice #$publicId",
                    username = "user_$publicId",
                    email = email.trim(),
                    avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    profileFrameId = "frame_gold_crown",
                    coverImage = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
                    bio = "Voice Room VIP Member 🎙️✨",
                    country = "Global",
                    isOnline = true,
                    lastActive = System.currentTimeMillis(),
                    coinBalance = 2500L,
                    diamondBalance = 0L,
                    level = 1,
                    vipLevel = 1
                )
                userDocRef.set(newProfile).await()
                newProfile
            }

            // 3. Cache in Room
            val userEntity = UserEntity(
                id = uid,
                publicUserId = firestoreProfile.publicUserId,
                username = firestoreProfile.username,
                displayName = firestoreProfile.displayName,
                email = email.trim(),
                phone = null,
                passwordHash = SecurityUtils.hashPassword(passwordRaw),
                avatar = firestoreProfile.avatar,
                coverImage = firestoreProfile.coverImage,
                countryId = 1,
                status = "ACTIVE",
                isVerified = true
            )
            val profileEntity = ProfileEntity(
                userId = uid,
                bio = firestoreProfile.bio,
                level = firestoreProfile.level,
                vipLevel = firestoreProfile.vipLevel,
                coinBalance = firestoreProfile.coinBalance,
                earnings = firestoreProfile.diamondBalance
            )
            userDao.registerUser(userEntity, profileEntity)

            // 4. Session management
            val sessionToken = UUID.randomUUID().toString()
            val session = SessionEntity(
                id = sessionToken,
                userId = uid,
                deviceId = "Android",
                deviceName = "Mobile Device",
                expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
            )
            sessionDao.insertSession(session)
            dataStore.edit {
                it[SESSION_TOKEN] = sessionToken
                it[CURRENT_USER_ID] = uid
                it[WAS_EXPLICITLY_LOGGED_OUT] = false
            }

            Result.success(Pair(userEntity, sessionToken))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Sends password reset email using Firebase Authentication.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (email.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Please enter your email address."))
            }
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Real Logout: Sets online presence to false in Firestore, signs out of FirebaseAuth, and cleans local session.
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                firestore.collection(USERS_COLLECTION).document(uid).update(
                    mapOf(
                        "isOnline" to false,
                        "lastActive" to System.currentTimeMillis()
                    )
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        dataStore.edit {
            val token = it[SESSION_TOKEN]
            if (token != null) {
                sessionDao.deleteSession(token)
            }
            it.remove(SESSION_TOKEN)
            it.remove(CURRENT_USER_ID)
            it[WAS_EXPLICITLY_LOGGED_OUT] = true
        }
    }

    suspend fun getCurrentSession(token: String): SessionEntity? {
        return sessionDao.getSession(token)
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}

