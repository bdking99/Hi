package com.example.data.repository

import android.util.Log
import com.example.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class AdminRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) {
    companion object {
        private const val TAG = "AdminRepository"
        const val USERS_COLLECTION = "users"
        const val WALLETS_COLLECTION = "wallets"
        const val TRANSACTIONS_COLLECTION = "walletTransactions"
        const val GIFTS_COLLECTION = "gifts"
        const val FRAMES_COLLECTION = "profileFrames"
        const val ROOMS_COLLECTION = "rooms"
        const val REPORTS_COLLECTION = "reports"
        const val GAMES_COLLECTION = "games"
        const val AUDIT_LOGS_COLLECTION = "auditLogs"
        const val FEATURE_FLAGS_COLLECTION = "featureFlags"
        const val SYSTEM_SETTINGS_DOC = "system/settings"
    }

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    // ==========================================
    // 1. SYSTEM METRICS AGGREGATION
    // ==========================================

    suspend fun fetchSystemMetrics(): Result<SystemMetrics> = withContext(Dispatchers.IO) {
        try {
            val usersSnapshot = firestore.collection(USERS_COLLECTION).limit(500).get().await()
            val totalUsers = usersSnapshot.size().toLong()
            val onlineUsers = usersSnapshot.documents.count { doc ->
                val lastActive = doc.getLong("lastActiveAt") ?: 0L
                System.currentTimeMillis() - lastActive < 10 * 60 * 1000 // Active within last 10 mins
            }.toLong()

            val roomsSnapshot = firestore.collection(ROOMS_COLLECTION).whereEqualTo("isActive", true).get().await()
            val activeRooms = roomsSnapshot.size().toLong()

            val reportsSnapshot = firestore.collection(REPORTS_COLLECTION).whereEqualTo("status", "PENDING").get().await()
            val pendingReports = reportsSnapshot.size().toLong()

            val gamesSnapshot = firestore.collection(GAMES_COLLECTION).whereEqualTo("isActive", true).get().await()
            val activeGames = gamesSnapshot.size().toLong()

            val txSnapshot = firestore.collection(TRANSACTIONS_COLLECTION).limit(200).get().await()
            val totalTransactions = txSnapshot.size().toLong()
            var volume = 0L
            for (doc in txSnapshot.documents) {
                volume += (doc.getLong("amount") ?: 0L).coerceAtLeast(0L)
            }

            val metrics = SystemMetrics(
                totalUsers = totalUsers.coerceAtLeast(1L),
                onlineUsers = onlineUsers.coerceAtLeast(1L),
                activeRooms = activeRooms,
                activeVoiceSessions = activeRooms * 2,
                totalTransactions = totalTransactions,
                transactionVolume = volume,
                giftsSentToday = (totalTransactions / 2).coerceAtLeast(0L),
                pendingReports = pendingReports,
                activeGames = activeGames.coerceAtLeast(1L),
                serverStatus = "OPERATIONAL",
                lastAuditTimestamp = System.currentTimeMillis()
            )
            Result.success(metrics)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching metrics: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // 2. FEATURE FLAGS MANAGEMENT
    // ==========================================

    fun getFeatureFlagsStream(): Flow<List<FeatureFlag>> = callbackFlow {
        val listener = firestore.collection(FEATURE_FLAGS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DefaultFeatureFlags.DEFAULT_LIST)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val flags = snapshot.documents.mapNotNull { it.toObject(FeatureFlag::class.java) }
                    trySend(flags)
                } else {
                    seedDefaultFeatureFlags()
                    trySend(DefaultFeatureFlags.DEFAULT_LIST)
                }
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    private fun seedDefaultFeatureFlags() {
        for (flag in DefaultFeatureFlags.DEFAULT_LIST) {
            firestore.collection(FEATURE_FLAGS_COLLECTION).document(flag.featureId).set(flag, SetOptions.merge())
        }
    }

    suspend fun toggleFeatureFlag(
        featureId: String,
        enabled: Boolean,
        adminUid: String,
        adminRole: String,
        adminDisplayName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!ModerationRoles.isAdminOrAbove(adminRole)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin privilege required."))
            }

            val updates = mapOf(
                "isEnabled" to enabled,
                "lastUpdatedBy" to adminDisplayName,
                "lastUpdatedAt" to System.currentTimeMillis()
            )
            firestore.collection(FEATURE_FLAGS_COLLECTION).document(featureId).update(updates).await()

            recordAuditLog(
                actorUid = adminUid,
                actorRole = adminRole,
                actorDisplayName = adminDisplayName,
                action = AdminActionTypes.FEATURE_FLAG_TOGGLE,
                targetType = "FEATURE_FLAG",
                targetId = featureId,
                reason = "Feature toggled to ${if (enabled) "ENABLED" else "DISABLED"}"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 3. SYSTEM SETTINGS
    // ==========================================

    fun getSystemSettingsStream(): Flow<SystemSettingsRecord> = callbackFlow {
        val listener = firestore.document(SYSTEM_SETTINGS_DOC)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(SystemSettingsRecord())
                    return@addSnapshotListener
                }
                val settings = snapshot.toObject(SystemSettingsRecord::class.java) ?: SystemSettingsRecord()
                trySend(settings)
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun updateSystemSettings(
        settings: SystemSettingsRecord,
        adminUid: String,
        adminRole: String,
        adminDisplayName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!ModerationRoles.isAdminOrAbove(adminRole)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin privilege required."))
            }

            firestore.document(SYSTEM_SETTINGS_DOC).set(settings, SetOptions.merge()).await()

            recordAuditLog(
                actorUid = adminUid,
                actorRole = adminRole,
                actorDisplayName = adminDisplayName,
                action = AdminActionTypes.SETTINGS_UPDATE,
                targetType = "SYSTEM",
                targetId = "settings",
                reason = "Updated system settings (maintenance: ${settings.maintenanceMode})"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 4. USER ADMINISTRATION & ROLE ASSIGNMENT
    // ==========================================

    suspend fun searchUsers(searchQuery: String = "", limit: Long = 50): Result<List<FirebaseUserProfile>> = withContext(Dispatchers.IO) {
        try {
            val query = if (searchQuery.isNotBlank()) {
                val clean = searchQuery.trim()
                firestore.collection(USERS_COLLECTION)
                    .orderBy("publicUserId")
                    .startAt(clean)
                    .endAt(clean + "\uf8ff")
                    .limit(limit)
            } else {
                firestore.collection(USERS_COLLECTION).limit(limit)
            }

            val snapshot = query.get().await()
            val list = snapshot.documents.mapNotNull { it.toObject(FirebaseUserProfile::class.java) }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(
        adminUid: String,
        adminRole: String,
        adminDisplayName: String,
        targetUid: String,
        newRole: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!ModerationRoles.isAdminOrAbove(adminRole)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin privilege required to modify roles."))
            }

            // Super Admin check for assigning Super Admin or Admin
            if ((newRole == ModerationRoles.SUPER_ADMIN || newRole == ModerationRoles.ADMIN) && adminRole != ModerationRoles.SUPER_ADMIN) {
                return@withContext Result.failure(SecurityException("Only Super Admins can promote users to Admin or Super Admin."))
            }

            firestore.collection(USERS_COLLECTION).document(targetUid).update("role", newRole).await()

            recordAuditLog(
                actorUid = adminUid,
                actorRole = adminRole,
                actorDisplayName = adminDisplayName,
                action = AdminActionTypes.ROLE_CHANGE,
                targetType = "USER",
                targetId = targetUid,
                reason = "Role changed to $newRole: $reason"
            )

            // Notify user
            notificationRepository.sendNotification(
                AppNotification(
                    recipientUid = targetUid,
                    senderUid = adminUid,
                    type = NotificationTypes.SYSTEM,
                    title = "Role Assignment Updated",
                    body = "Your platform access role is now $newRole. Reason: $reason",
                    referenceId = targetUid,
                    referenceType = "USER"
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 5. WALLET COIN ADJUSTMENTS
    // ==========================================

    suspend fun adjustUserCoins(
        adminUid: String,
        adminRole: String,
        adminDisplayName: String,
        targetUid: String,
        targetPublicId: String,
        amountDelta: Long,
        reason: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (!ModerationRoles.isAdminOrAbove(adminRole)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin role required for coin adjustments."))
            }

            if (amountDelta == 0L || reason.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Amount delta must be non-zero and reason is mandatory."))
            }

            val newBalance = firestore.runTransaction { tx ->
                val walletRef = firestore.collection(WALLETS_COLLECTION).document(targetUid)
                val userRef = firestore.collection(USERS_COLLECTION).document(targetUid)

                val walletDoc = tx.get(walletRef)
                val currentCoins = walletDoc.getLong("coinBalance") ?: 0L
                val updatedCoins = (currentCoins + amountDelta).coerceAtLeast(0L)

                tx.update(walletRef, "coinBalance", updatedCoins, "updatedAt", System.currentTimeMillis())
                tx.update(userRef, "coinBalance", updatedCoins)

                // Record transaction
                val txId = "tx_admin_${UUID.randomUUID()}"
                val transactionRecord = WalletTransaction(
                    transactionId = txId,
                    uid = targetUid,
                    publicUserId = targetPublicId,
                    type = if (amountDelta > 0) "ADMIN_CREDIT" else "ADMIN_DEBIT",
                    amount = kotlin.math.abs(amountDelta),
                    balanceBefore = currentCoins,
                    balanceAfter = updatedCoins,
                    referenceId = adminUid,
                    status = "SUCCESS",
                    description = "Admin adjustment: $reason"
                )
                tx.set(firestore.collection(TRANSACTIONS_COLLECTION).document(txId), transactionRecord)

                updatedCoins
            }.await()

            recordAuditLog(
                actorUid = adminUid,
                actorRole = adminRole,
                actorDisplayName = adminDisplayName,
                action = AdminActionTypes.WALLET_ADJUSTMENT,
                targetType = "WALLET",
                targetId = targetUid,
                reason = "Adjusted coins by $amountDelta. Reason: $reason",
                metadata = mapOf("amountDelta" to amountDelta, "newBalance" to newBalance)
            )

            // Notify recipient
            notificationRepository.sendNotification(
                AppNotification(
                    recipientUid = targetUid,
                    senderUid = adminUid,
                    type = NotificationTypes.SYSTEM,
                    title = if (amountDelta > 0) "Coins Credited" else "Coins Adjusted",
                    body = if (amountDelta > 0) "You received $amountDelta coins from administration: $reason" else "$amountDelta coins were adjusted: $reason",
                    referenceId = targetUid,
                    referenceType = "WALLET"
                )
            )

            Result.success(newBalance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 6. GIFT & FRAME CATALOG MANAGEMENT
    // ==========================================

    suspend fun saveGift(
        adminUid: String,
        adminRole: String,
        adminDisplayName: String,
        gift: CatalogGift
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!ModerationRoles.isAdminOrAbove(adminRole)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin privilege required."))
            }

            val giftId = gift.giftId.ifBlank { "gift_${UUID.randomUUID()}" }
            val itemToSave = gift.copy(giftId = giftId, updatedAt = System.currentTimeMillis())

            firestore.collection(GIFTS_COLLECTION).document(giftId).set(itemToSave, SetOptions.merge()).await()

            recordAuditLog(
                actorUid = adminUid,
                actorRole = adminRole,
                actorDisplayName = adminDisplayName,
                action = AdminActionTypes.GIFT_MANAGED,
                targetType = "GIFT",
                targetId = giftId,
                reason = "Created/Updated gift ${gift.name} (${gift.coinPrice} coins)"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProfileFrame(
        adminUid: String,
        adminRole: String,
        adminDisplayName: String,
        frame: ProfileFrameItem
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!ModerationRoles.isAdminOrAbove(adminRole)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin privilege required."))
            }

            val frameId = frame.frameId.ifBlank { "frame_${UUID.randomUUID()}" }
            val itemToSave = frame.copy(frameId = frameId, updatedAt = System.currentTimeMillis())

            firestore.collection(FRAMES_COLLECTION).document(frameId).set(itemToSave, SetOptions.merge()).await()

            recordAuditLog(
                actorUid = adminUid,
                actorRole = adminRole,
                actorDisplayName = adminDisplayName,
                action = AdminActionTypes.FRAME_MANAGED,
                targetType = "FRAME",
                targetId = frameId,
                reason = "Created/Updated frame ${frame.name} (${frame.requiredCoins} coins)"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 7. ROOM SUPERVISION & AUDIT LOG STREAM
    // ==========================================

    suspend fun terminateRoom(
        adminUid: String,
        adminRole: String,
        adminDisplayName: String,
        roomId: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!ModerationRoles.isModeratorOrAbove(adminRole)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Moderator or Admin privilege required."))
            }

            firestore.collection(ROOMS_COLLECTION).document(roomId).update(
                mapOf(
                    "isActive" to false,
                    "isLocked" to true,
                    "terminatedReason" to reason
                )
            ).await()

            recordAuditLog(
                actorUid = adminUid,
                actorRole = adminRole,
                actorDisplayName = adminDisplayName,
                action = AdminActionTypes.ROOM_TERMINATED,
                targetType = "ROOM",
                targetId = roomId,
                reason = "Room closed by moderator: $reason"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllAuditLogsStream(limit: Long = 100): Flow<List<AdminAuditLogRecord>> = callbackFlow {
        val query = firestore.collection(AUDIT_LOGS_COLLECTION)
            .limit(limit)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val logs = snapshot?.documents?.mapNotNull { it.toObject(AdminAuditLogRecord::class.java) } ?: emptyList()
            trySend(logs.sortedByDescending { it.createdAt })
        }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun recordAuditLog(
        actorUid: String,
        actorRole: String,
        actorDisplayName: String,
        action: String,
        targetType: String,
        targetId: String,
        reason: String,
        metadata: Map<String, Any?> = emptyMap()
    ) {
        try {
            val logId = "audit_${UUID.randomUUID()}"
            val record = AdminAuditLogRecord(
                logId = logId,
                actorUid = actorUid,
                actorRole = actorRole,
                actorDisplayName = actorDisplayName,
                action = action,
                targetType = targetType,
                targetId = targetId,
                reason = reason,
                metadata = metadata,
                createdAt = System.currentTimeMillis()
            )
            firestore.collection(AUDIT_LOGS_COLLECTION).document(logId).set(record)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record audit log: ${e.message}", e)
        }
    }
}
