package com.example.data.repository

import android.app.Activity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.data.local.dao.SessionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.ProfileEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.FirebaseRtdbUser
import com.example.data.model.FirebaseUserProfile
import com.example.data.model.UserWallet
import com.example.utils.FirebaseAuthUtils
import com.example.utils.SecurityUtils
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class AuthRepository(
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        const val RTDB_URL = "https://great-voice-chat-default-rtdb.firebaseio.com"
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val WAS_EXPLICITLY_LOGGED_OUT = booleanPreferencesKey("was_explicitly_logged_out")

        const val RTDB_USERS = "users"
        const val RTDB_USERNAMES = "usernames"
        const val FIRESTORE_USERS = "users"
        const val FIRESTORE_WALLETS = "wallets"
    }

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val rtdb: FirebaseDatabase get() = FirebaseDatabase.getInstance(RTDB_URL)
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    val currentSessionToken: Flow<String?> = dataStore.data.map { it[SESSION_TOKEN] }
    val currentUserIdFlow: Flow<String?> = dataStore.data.map { it[CURRENT_USER_ID] }

    val currentFirebaseUser: FirebaseUser?
        get() = auth.currentUser

    val currentFirebaseUid: String?
        get() = auth.currentUser?.uid

    suspend fun isLoggedIn(): Boolean {
        val firebaseUser = auth.currentUser
        return firebaseUser != null
    }

    /**
     * Checks if a username is available in Firebase Realtime Database.
     * Path: usernames/{username} -> stores uid.
     */
    suspend fun isUsernameAvailable(username: String, currentUid: String? = null): Boolean = withContext(Dispatchers.IO) {
        val cleanHandle = username.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' }
        if (cleanHandle.isBlank()) return@withContext false

        try {
            val snapshot = suspendCancellableCoroutine<DataSnapshot?> { cont ->
                val ref = rtdb.reference.child(RTDB_USERNAMES).child(cleanHandle)
                val listener = object : ValueEventListener {
                    override fun onDataChange(snap: DataSnapshot) {
                        if (cont.isActive) cont.resume(snap)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
                ref.addListenerForSingleValueEvent(listener)
            }

            if (snapshot != null && snapshot.exists()) {
                val existingUid = snapshot.getValue(String::class.java)
                return@withContext existingUid == null || existingUid == currentUid
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }

    /**
     * Reserves a unique username in Realtime Database.
     */
    private suspend fun reserveUsername(username: String, uid: String) = withContext(Dispatchers.IO) {
        val cleanHandle = username.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' }
        if (cleanHandle.isNotBlank()) {
            try {
                rtdb.reference.child(RTDB_USERNAMES).child(cleanHandle).setValue(uid).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Saves user profile to Firebase Realtime Database at `users/{uid}`.
     * Never stores password in Realtime Database.
     */
    suspend fun saveRtdbUserProfile(rtdbUser: FirebaseRtdbUser) = withContext(Dispatchers.IO) {
        try {
            val userMap = mapOf(
                "uid" to rtdbUser.uid,
                "name" to rtdbUser.name,
                "username" to rtdbUser.username,
                "email" to rtdbUser.email,
                "phone" to rtdbUser.phone,
                "photoUrl" to rtdbUser.photoUrl,
                "createdAt" to rtdbUser.createdAt,
                "provider" to rtdbUser.provider
            )
            rtdb.reference.child(RTDB_USERS).child(rtdbUser.uid).setValue(userMap).await()
            if (rtdbUser.username.isNotBlank()) {
                reserveUsername(rtdbUser.username, rtdbUser.uid)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reads user profile from Firebase Realtime Database at `users/{uid}`.
     */
    suspend fun getRtdbUserProfile(uid: String): FirebaseRtdbUser? = withContext(Dispatchers.IO) {
        try {
            val snap = suspendCancellableCoroutine<DataSnapshot?> { cont ->
                val ref = rtdb.reference.child(RTDB_USERS).child(uid)
                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (cont.isActive) cont.resume(snapshot)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        if (cont.isActive) cont.resume(null)
                    }
                })
            }
            if (snap != null && snap.exists()) {
                FirebaseRtdbUser(
                    uid = snap.child("uid").getValue(String::class.java) ?: uid,
                    name = snap.child("name").getValue(String::class.java) ?: "",
                    username = snap.child("username").getValue(String::class.java) ?: "",
                    email = snap.child("email").getValue(String::class.java) ?: "",
                    phone = snap.child("phone").getValue(String::class.java) ?: "",
                    photoUrl = snap.child("photoUrl").getValue(String::class.java) ?: "",
                    createdAt = snap.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
                    provider = snap.child("provider").getValue(String::class.java) ?: "email"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Syncs user profile with Firestore and local Room database.
     */
    private suspend fun syncProfileToServices(
        uid: String,
        name: String,
        username: String,
        email: String,
        phone: String,
        photoUrl: String,
        provider: String
    ) = withContext(Dispatchers.IO) {
        val avatar = photoUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" }
        val numericPublicId = ((uid.hashCode().toLong() and 0x7FFFFFFF) % 9000000L + 1000000L).toString()

        // 1. Sync to Firestore `users/{uid}`
        try {
            val firestoreDoc = firestore.collection(FIRESTORE_USERS).document(uid)
            val docSnap = firestoreDoc.get().await()
            val existingProfile = if (docSnap.exists()) docSnap.toObject(FirebaseUserProfile::class.java) else null

            val profileToSave = FirebaseUserProfile(
                userId = uid,
                publicUserId = existingProfile?.publicUserId?.ifBlank { numericPublicId } ?: numericPublicId,
                displayName = name.ifBlank { "VIP Voice #$numericPublicId" },
                username = username.ifBlank { "user_$numericPublicId" },
                email = email,
                phone = phone.ifBlank { null },
                avatar = avatar,
                profileFrameId = existingProfile?.profileFrameId ?: "frame_gold_crown",
                coverImage = existingProfile?.coverImage ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
                bio = existingProfile?.bio ?: "Voice Room VIP Member 🎙️✨",
                country = "Global",
                gender = "Not specified",
                accountStatus = "ACTIVE",
                createdAt = existingProfile?.createdAt ?: System.currentTimeMillis(),
                isOnline = true,
                lastActive = System.currentTimeMillis(),
                coinBalance = existingProfile?.coinBalance ?: 2500L,
                diamondBalance = existingProfile?.diamondBalance ?: 0L,
                level = existingProfile?.level ?: 1,
                vipLevel = existingProfile?.vipLevel ?: 1
            )
            firestoreDoc.set(profileToSave, SetOptions.merge()).await()

            // 2. Initialize Firestore Wallet if needed
            val walletDoc = firestore.collection(FIRESTORE_WALLETS).document(uid)
            val walletSnap = walletDoc.get().await()
            if (!walletSnap.exists()) {
                val initialWallet = UserWallet(
                    uid = uid,
                    publicUserId = numericPublicId,
                    coinBalance = 2500L,
                    lifetimePurchasedCoins = 0L,
                    lifetimeReceivedCoins = 0L,
                    lifetimeSpentCoins = 0L,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                walletDoc.set(initialWallet).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Cache into Room database
        try {
            val userEntity = UserEntity(
                id = uid,
                publicUserId = numericPublicId,
                username = username,
                displayName = name,
                email = email.ifBlank { null },
                phone = phone.ifBlank { null },
                passwordHash = null,
                avatar = avatar,
                coverImage = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
                countryId = 1,
                status = "ACTIVE",
                isVerified = true
            )
            val profileEntity = ProfileEntity(
                userId = uid,
                bio = "Voice Room VIP Member 🎙️✨",
                level = 1,
                vipLevel = 1,
                coinBalance = 2500L,
                earnings = 0L
            )
            userDao.registerUser(userEntity, profileEntity)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Update session DataStore
        val sessionToken = UUID.randomUUID().toString()
        val session = SessionEntity(
            id = sessionToken,
            userId = uid,
            deviceId = "Android",
            deviceName = "Mobile Device",
            expiresAt = System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000
        )
        sessionDao.insertSession(session)
        dataStore.edit {
            it[SESSION_TOKEN] = sessionToken
            it[CURRENT_USER_ID] = uid
            it[WAS_EXPLICITLY_LOGGED_OUT] = false
        }
    }

    /**
     * Real Firebase Email/Password Registration
     */
    suspend fun registerWithEmail(
        name: String,
        username: String,
        email: String,
        phone: String,
        passwordRaw: String,
        photoUrl: String? = null
    ): Result<FirebaseRtdbUser> = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanUsername = username.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' }
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()

        if (cleanName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Name is required."))
        }
        if (cleanUsername.length < 3) {
            return@withContext Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
        }
        if (!isUsernameAvailable(cleanUsername)) {
            return@withContext Result.failure(IllegalArgumentException("Username is already taken. Please choose another."))
        }
        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (passwordRaw.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password should be at least 6 characters."))
        }

        try {
            // 1. Create in Firebase Authentication
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, passwordRaw).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user creation returned null")
            val uid = firebaseUser.uid

            val avatar = photoUrl?.ifBlank { null } ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"

            // 2. Save profile in Firebase Realtime Database
            val rtdbUser = FirebaseRtdbUser(
                uid = uid,
                name = cleanName,
                username = cleanUsername,
                email = cleanEmail,
                phone = cleanPhone,
                photoUrl = avatar,
                createdAt = System.currentTimeMillis(),
                provider = "email"
            )
            saveRtdbUserProfile(rtdbUser)

            // 3. Sync to Firestore & Room & Session
            syncProfileToServices(
                uid = uid,
                name = cleanName,
                username = cleanUsername,
                email = cleanEmail,
                phone = cleanPhone,
                photoUrl = avatar,
                provider = "email"
            )

            Result.success(rtdbUser)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(FirebaseAuthUtils.getFriendlyErrorMessage(e)))
        }
    }

    /**
     * Real Firebase Email/Password Login
     */
    suspend fun loginWithEmail(email: String, passwordRaw: String): Result<FirebaseRtdbUser> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (passwordRaw.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your password."))
        }

        try {
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, passwordRaw).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase sign in returned null")
            val uid = firebaseUser.uid

            // Fetch Realtime Database profile or create default if first time
            var rtdbUser = getRtdbUserProfile(uid)
            if (rtdbUser == null) {
                val fallbackHandle = cleanEmail.substringBefore("@").lowercase().filter { it.isLetterOrDigit() || it == '_' }
                val handle = if (isUsernameAvailable(fallbackHandle, uid)) fallbackHandle else "user_${uid.take(6)}"
                rtdbUser = FirebaseRtdbUser(
                    uid = uid,
                    name = firebaseUser.displayName ?: fallbackHandle.replaceFirstChar { it.uppercase() },
                    username = handle,
                    email = cleanEmail,
                    phone = firebaseUser.phoneNumber ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    createdAt = System.currentTimeMillis(),
                    provider = "email"
                )
                saveRtdbUserProfile(rtdbUser)
            }

            syncProfileToServices(
                uid = uid,
                name = rtdbUser.name,
                username = rtdbUser.username,
                email = rtdbUser.email,
                phone = rtdbUser.phone,
                photoUrl = rtdbUser.photoUrl,
                provider = rtdbUser.provider
            )

            Result.success(rtdbUser)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(FirebaseAuthUtils.getFriendlyErrorMessage(e)))
        }
    }

    /**
     * Real Firebase Google Sign-In with ID Token
     */
    suspend fun signInWithGoogle(
        idToken: String,
        email: String? = null,
        displayName: String? = null,
        photoUrl: String? = null
    ): Result<FirebaseRtdbUser> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Google sign-in returned null user")
            val uid = firebaseUser.uid

            var rtdbUser = getRtdbUserProfile(uid)
            if (rtdbUser == null) {
                val userEmail = firebaseUser.email ?: email ?: ""
                val userName = firebaseUser.displayName ?: displayName ?: "Google User"
                val baseHandle = userEmail.substringBefore("@").lowercase().filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "user_${uid.take(6)}" }
                val handle = if (isUsernameAvailable(baseHandle, uid)) baseHandle else "user_${uid.take(6)}"
                val userPhoto = firebaseUser.photoUrl?.toString() ?: photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"

                rtdbUser = FirebaseRtdbUser(
                    uid = uid,
                    name = userName,
                    username = handle,
                    email = userEmail,
                    phone = firebaseUser.phoneNumber ?: "",
                    photoUrl = userPhoto,
                    createdAt = System.currentTimeMillis(),
                    provider = "google"
                )
                saveRtdbUserProfile(rtdbUser)
            }

            syncProfileToServices(
                uid = uid,
                name = rtdbUser.name,
                username = rtdbUser.username,
                email = rtdbUser.email,
                phone = rtdbUser.phone,
                photoUrl = rtdbUser.photoUrl,
                provider = "google"
            )

            Result.success(rtdbUser)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(FirebaseAuthUtils.getFriendlyErrorMessage(e)))
        }
    }

    /**
     * Sends Firebase Phone OTP
     */
    fun sendPhoneVerificationCode(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber.trim())
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Signs in with Phone OTP Credential
     */
    suspend fun verifyPhoneOtpAndSignIn(
        verificationId: String,
        smsCode: String,
        name: String? = null,
        username: String? = null,
        email: String? = null,
        photoUrl: String? = null
    ): Result<FirebaseRtdbUser> = withContext(Dispatchers.IO) {
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, smsCode.trim())
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Phone sign-in returned null user")
            val uid = firebaseUser.uid
            val phone = firebaseUser.phoneNumber ?: ""

            var rtdbUser = getRtdbUserProfile(uid)
            if (rtdbUser == null) {
                val cleanName = name?.trim()?.ifBlank { "Voice User" } ?: "Voice User"
                val baseHandle = username?.trim()?.lowercase()?.filter { it.isLetterOrDigit() || it == '_' } ?: "user_${uid.take(6)}"
                val cleanHandle = if (isUsernameAvailable(baseHandle, uid)) baseHandle else "user_${uid.take(6)}"
                val avatar = photoUrl?.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" } ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"

                rtdbUser = FirebaseRtdbUser(
                    uid = uid,
                    name = cleanName,
                    username = cleanHandle,
                    email = email?.trim() ?: "",
                    phone = phone,
                    photoUrl = avatar,
                    createdAt = System.currentTimeMillis(),
                    provider = "phone"
                )
                saveRtdbUserProfile(rtdbUser)
            } else if (email != null && email.isNotBlank() && rtdbUser.email.isBlank()) {
                rtdbUser = rtdbUser.copy(email = email.trim())
                saveRtdbUserProfile(rtdbUser)
            }

            syncProfileToServices(
                uid = uid,
                name = rtdbUser.name,
                username = rtdbUser.username,
                email = rtdbUser.email,
                phone = rtdbUser.phone,
                photoUrl = rtdbUser.photoUrl,
                provider = "phone"
            )

            Result.success(rtdbUser)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(FirebaseAuthUtils.getFriendlyErrorMessage(e)))
        }
    }

    /**
     * Sends password reset email using Firebase Authentication.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim()
            if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
            }
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(FirebaseAuthUtils.getFriendlyErrorMessage(e)))
        }
    }

    /**
     * Real Logout: Completely signs out from Firebase Authentication and clears local session.
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                firestore.collection(FIRESTORE_USERS).document(uid).update(
                    mapOf("isOnline" to false, "lastActive" to System.currentTimeMillis())
                ).await()
            } catch (ign: Exception) {}
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
