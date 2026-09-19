package com.example.data.repository

import com.example.data.local.dao.UserDao
import com.example.data.local.entity.ProfileEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.FirebaseUserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(private val userDao: UserDao) {
    private val usersCollection = "users"

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Room local persistence flows
    fun getUserFlow(userId: String): Flow<UserEntity?> = userDao.getUserFlow(userId)
    fun getProfileFlow(userId: String): Flow<ProfileEntity?> = userDao.getProfileFlow(userId)
    
    suspend fun getUser(userId: String): UserEntity? = userDao.getUser(userId)

    suspend fun addCoins(userId: String, deltaCoins: Long) {
        userDao.addCoins(userId, deltaCoins)
    }

    suspend fun updateBalances(userId: String, coins: Long, earnings: Long) {
        userDao.updateBalances(userId, coins, earnings)
    }

    suspend fun updateProfile(userId: String, displayName: String, bio: String, avatar: String?, coverImage: String?) {
        userDao.updateUserInfo(userId, displayName, avatar, coverImage)
        userDao.updateBio(userId, bio)

        // Also sync profile changes to Firestore if connected
        try {
            val firestore = getFirestore() ?: return
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName,
                "bio" to bio
            )
            if (avatar != null) updates["avatar"] = avatar
            if (coverImage != null) updates["coverImage"] = coverImage
            firestore.collection(usersCollection).document(userId).set(updates, SetOptions.merge())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Firestore query: Real-time stream to fetch public profile data for any user.
     */
    fun getPublicProfileStream(userId: String): Flow<FirebaseUserProfile?> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(getFallbackProfile(userId))
            close()
            return@callbackFlow
        }

        try {
            val docRef = firestore.collection(usersCollection).document(userId)
            val listener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(getFallbackProfile(userId))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(FirebaseUserProfile::class.java)
                    trySend(profile)
                } else {
                    val fallback = getFallbackProfile(userId)
                    trySend(fallback)
                }
            }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(getFallbackProfile(userId))
            close()
        }
    }

    /**
     * Firestore query: One-shot fetch of public profile data by user ID.
     */
    suspend fun getPublicProfile(userId: String): FirebaseUserProfile? {
        val firestore = getFirestore() ?: return getFallbackProfile(userId)
        return try {
            val snapshot = firestore.collection(usersCollection).document(userId).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(FirebaseUserProfile::class.java)
            } else {
                getFallbackProfile(userId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getFallbackProfile(userId)
        }
    }

    private fun getFallbackProfile(userId: String): FirebaseUserProfile {
        return FirebaseUserProfile(
            userId = userId,
            publicUserId = if (userId.length > 6) userId.takeLast(6) else "102839",
            displayName = "Voice Enthusiast",
            username = "user_$userId",
            avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            coverImage = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
            bio = "Great Voice Room VIP member 🎙️✨",
            level = 12,
            vipLevel = 2,
            coinBalance = 2500L,
            diamondBalance = 840L,
            followersCount = 230,
            followingCount = 145
        )
    }
}
