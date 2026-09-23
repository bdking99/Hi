package com.example.data.repository

import android.os.Build
import android.util.Log
import com.example.data.model.AppNotification
import com.example.data.model.NotificationPreferences
import com.example.data.model.NotificationTypes
import com.example.data.model.UserDeviceToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class NotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "NotificationRepository"
        const val NOTIFICATIONS_COLLECTION = "notifications"
        const val USER_DEVICES_COLLECTION = "userDevices"
        const val PREFERENCES_COLLECTION = "notificationPreferences"
    }

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    /**
     * Send a real notification to a target user, respecting the user's notification preferences.
     */
    suspend fun sendNotification(notification: AppNotification): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (notification.recipientUid.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Recipient UID cannot be blank"))
            }

            // Check if recipient has enabled this notification type in preferences
            val prefs = getPreferencesOnce(notification.recipientUid)
            if (!isNotificationAllowedByPrefs(notification.type, prefs)) {
                Log.d(TAG, "Notification type ${notification.type} suppressed by recipient preferences")
                return@withContext Result.success(Unit)
            }

            val docId = if (notification.notificationId.isNotBlank()) {
                notification.notificationId
            } else {
                "notif_${UUID.randomUUID()}"
            }

            val finalNotification = notification.copy(
                notificationId = docId,
                createdAt = System.currentTimeMillis()
            )

            firestore.collection(NOTIFICATIONS_COLLECTION)
                .document(docId)
                .set(finalNotification)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of notifications for the current user, ordered by creation time descending.
     */
    fun getNotificationsStream(recipientUid: String, limit: Long = 60): Flow<List<AppNotification>> = callbackFlow {
        val effectiveUid = if (recipientUid.isNotBlank()) recipientUid else (auth.currentUser?.uid ?: "")
        if (effectiveUid.isBlank() || auth.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = firestore.collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("recipientUid", effectiveUid)
            .limit(limit)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Notice listening to notifications: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val notifications = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AppNotification::class.java)
                }.sortedByDescending { it.createdAt }
                trySend(notifications)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Real-time stream of unread notification count.
     */
    fun getUnreadCountStream(recipientUid: String): Flow<Int> = callbackFlow {
        val effectiveUid = if (recipientUid.isNotBlank()) recipientUid else (auth.currentUser?.uid ?: "")
        if (effectiveUid.isBlank() || auth.currentUser == null) {
            trySend(0)
            awaitClose {}
            return@callbackFlow
        }

        val query = firestore.collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("recipientUid", effectiveUid)
            .whereEqualTo("isRead", false)
            .limit(100)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(0)
                return@addSnapshotListener
            }
            trySend(snapshot?.size() ?: 0)
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Mark a single notification as read.
     * Enforces that the notification belongs to the current user.
     */
    suspend fun markAsRead(notificationId: String, currentUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (notificationId.isBlank() || currentUid.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Invalid notification or user ID"))
            }

            val docRef = firestore.collection(NOTIFICATIONS_COLLECTION).document(notificationId)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                return@withContext Result.failure(Exception("Notification not found"))
            }

            val notif = doc.toObject(AppNotification::class.java)
            if (notif?.recipientUid != currentUid) {
                return@withContext Result.failure(SecurityException("Unauthorized access to notification"))
            }

            docRef.update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Mark all unread notifications for the user as read.
     */
    suspend fun markAllAsRead(currentUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (currentUid.isBlank()) return@withContext Result.failure(IllegalArgumentException("Invalid user ID"))

            val unreadSnapshot = firestore.collection(NOTIFICATIONS_COLLECTION)
                .whereEqualTo("recipientUid", currentUid)
                .whereEqualTo("isRead", false)
                .limit(100)
                .get()
                .await()

            if (!unreadSnapshot.isEmpty) {
                val batch = firestore.batch()
                for (doc in unreadSnapshot.documents) {
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all notifications as read: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a single notification.
     */
    suspend fun deleteNotification(notificationId: String, currentUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection(NOTIFICATIONS_COLLECTION).document(notificationId)
            val doc = docRef.get().await()
            if (doc.exists() && doc.getString("recipientUid") == currentUid) {
                docRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register or update FCM device push token.
     */
    suspend fun registerDeviceToken(uid: String, token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (uid.isBlank() || token.isBlank()) return@withContext Result.failure(IllegalArgumentException("Blank uid or token"))

            val deviceId = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.ID}".replace(" ", "_")
            val record = UserDeviceToken(
                deviceId = deviceId,
                token = token,
                platform = "Android",
                appVersion = "1.0.0",
                enabled = true,
                lastSeenAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )

            firestore.collection(USER_DEVICES_COLLECTION)
                .document(uid)
                .collection("devices")
                .document(deviceId)
                .set(record, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering device token: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Stream notification preferences.
     */
    fun getNotificationPreferencesStream(uid: String): Flow<NotificationPreferences> = callbackFlow {
        if (uid.isBlank()) {
            trySend(NotificationPreferences())
            awaitClose {}
            return@callbackFlow
        }

        val docRef = firestore.collection(PREFERENCES_COLLECTION).document(uid)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(NotificationPreferences())
                return@addSnapshotListener
            }
            val prefs = snapshot.toObject(NotificationPreferences::class.java) ?: NotificationPreferences()
            trySend(prefs)
        }

        awaitClose { registration.remove() }
    }

    /**
     * Update notification preferences.
     */
    suspend fun updateNotificationPreferences(uid: String, preferences: NotificationPreferences): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (uid.isBlank()) return@withContext Result.failure(IllegalArgumentException("Blank uid"))
            firestore.collection(PREFERENCES_COLLECTION)
                .document(uid)
                .set(preferences, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getPreferencesOnce(uid: String): NotificationPreferences {
        return try {
            val doc = firestore.collection(PREFERENCES_COLLECTION).document(uid).get().await()
            if (doc.exists()) doc.toObject(NotificationPreferences::class.java) ?: NotificationPreferences()
            else NotificationPreferences()
        } catch (e: Exception) {
            NotificationPreferences()
        }
    }

    private fun isNotificationAllowedByPrefs(type: String, prefs: NotificationPreferences): Boolean {
        return when (type) {
            NotificationTypes.NEW_MESSAGE -> prefs.messages
            NotificationTypes.INCOMING_CALL, NotificationTypes.MISSED_CALL -> prefs.calls
            NotificationTypes.GIFT_RECEIVED -> prefs.gifts
            NotificationTypes.FOLLOW, NotificationTypes.FOLLOW_REQUEST, NotificationTypes.FOLLOW_ACCEPTED -> prefs.followers
            NotificationTypes.ROOM_INVITATION, NotificationTypes.ROOM_EVENT -> prefs.roomInvitations
            NotificationTypes.GAME_INVITATION -> prefs.gameInvitations
            NotificationTypes.VIP_LEVEL_UP, NotificationTypes.NORMAL_LEVEL_UP, NotificationTypes.FRAME_UNLOCKED -> prefs.vipLevelUpdates
            NotificationTypes.SYSTEM -> prefs.systemNotifications
            NotificationTypes.MODERATION, NotificationTypes.REPORT_UPDATE -> prefs.moderationUpdates
            else -> true
        }
    }
}
