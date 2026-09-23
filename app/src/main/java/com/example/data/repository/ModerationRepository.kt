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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class ModerationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) {
    companion object {
        private const val TAG = "ModerationRepository"
        const val USERS_COLLECTION = "users"
        const val REPORTS_COLLECTION = "reports"
        const val MODERATION_LOGS_COLLECTION = "moderationLogs"
        const val APPEALS_COLLECTION = "appeals"
    }

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    // ==========================================
    // REPORT SYSTEM
    // ==========================================

    /**
     * Submit a report with rate limiting and duplicate protection.
     */
    suspend fun submitReport(
        reporterUid: String,
        targetType: String,
        targetId: String,
        targetUid: String,
        targetDisplayName: String,
        reason: String,
        description: String,
        evidence: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (reporterUid.isBlank() || targetUid.isBlank() || reason.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Required report parameters are missing"))
            }

            // Duplicate protection: check if reporter submitted a report on the same target within last 5 minutes
            val recentReports = firestore.collection(REPORTS_COLLECTION)
                .whereEqualTo("reporterUid", reporterUid)
                .whereEqualTo("targetUid", targetUid)
                .limit(5)
                .get()
                .await()

            val fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000
            val duplicate = recentReports.documents.any { doc ->
                val createdAt = doc.getLong("createdAt") ?: 0L
                val status = doc.getString("status") ?: ""
                createdAt > fiveMinutesAgo && (status == "PENDING" || status == "UNDER_REVIEW")
            }

            if (duplicate) {
                return@withContext Result.failure(IllegalStateException("A pending report for this user was recently submitted. Please wait."))
            }

            // Fetch reporter public ID
            val reporterDoc = firestore.collection(USERS_COLLECTION).document(reporterUid).get().await()
            val reporterPublicId = reporterDoc.getString("publicUserId") ?: ""

            val reportId = "rep_${UUID.randomUUID()}"
            val sanitizedDescription = description.take(500).trim()

            val report = ReportRecord(
                reportId = reportId,
                reporterUid = reporterUid,
                reporterPublicUserId = reporterPublicId,
                targetType = targetType,
                targetId = targetId.ifBlank { targetUid },
                targetUid = targetUid,
                targetDisplayName = targetDisplayName,
                reason = reason,
                description = sanitizedDescription,
                evidence = evidence.take(500),
                status = "PENDING",
                priority = if (reason in listOf(ReportCategories.HARASSMENT, ReportCategories.VIOLENCE, ReportCategories.SCAM_FRAUD)) "HIGH" else "MEDIUM",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection(REPORTS_COLLECTION).document(reportId).set(report).await()
            Result.success(reportId)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting report: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of reports submitted by the current user.
     */
    fun getMyReportsStream(reporterUid: String): Flow<List<ReportRecord>> = callbackFlow {
        if (reporterUid.isBlank() || auth.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = firestore.collection(REPORTS_COLLECTION)
            .whereEqualTo("reporterUid", reporterUid)
            .limit(50)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(ReportRecord::class.java) } ?: emptyList()
            trySend(list.sortedByDescending { it.createdAt })
        }

        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of all reports for Moderator Dashboard.
     */
    fun getAllReportsStream(statusFilter: String? = null): Flow<List<ReportRecord>> = callbackFlow {
        if (auth.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val baseQuery = firestore.collection(REPORTS_COLLECTION).limit(100)

        val registration = baseQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Notice listening to reports: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            var list = snapshot?.documents?.mapNotNull { it.toObject(ReportRecord::class.java) } ?: emptyList()
            if (!statusFilter.isNullOrBlank() && statusFilter != "ALL") {
                list = list.filter { it.status.equals(statusFilter, ignoreCase = true) }
            }
            trySend(list.sortedByDescending { it.createdAt })
        }

        awaitClose { registration.remove() }
    }

    /**
     * Update report status and notify reporter.
     */
    suspend fun updateReportStatus(
        reportId: String,
        adminUid: String,
        newStatus: String,
        resolutionNotes: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection(REPORTS_COLLECTION).document(reportId)
            val doc = docRef.get().await()
            if (!doc.exists()) return@withContext Result.failure(Exception("Report not found"))

            val report = doc.toObject(ReportRecord::class.java) ?: return@withContext Result.failure(Exception("Invalid report"))

            val updates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "assignedAdminUid" to adminUid,
                "resolutionNotes" to resolutionNotes,
                "updatedAt" to System.currentTimeMillis()
            )
            if (newStatus in listOf("ACTION_TAKEN", "RESOLVED", "DISMISSED", "NO_ACTION")) {
                updates["resolvedAt"] = System.currentTimeMillis()
            }

            docRef.update(updates).await()

            // Notify reporter of status update
            notificationRepository.sendNotification(
                AppNotification(
                    recipientUid = report.reporterUid,
                    senderUid = adminUid,
                    type = NotificationTypes.REPORT_UPDATE,
                    title = "Report Update",
                    body = "Your report regarding ${report.targetDisplayName.ifBlank { "a user" }} is now $newStatus.",
                    referenceId = reportId,
                    referenceType = "REPORT"
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // MODERATION ACTIONS
    // ==========================================

    /**
     * Execute a moderation action on a user.
     */
    suspend fun executeModerationAction(
        actorUid: String,
        actorRole: String,
        actorDisplayName: String,
        targetUid: String,
        action: String,
        reason: String,
        durationMs: Long? = null,
        reportId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (actorUid.isBlank() || targetUid.isBlank() || action.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Missing moderation action params"))
            }

            val targetDocRef = firestore.collection(USERS_COLLECTION).document(targetUid)
            val targetDoc = targetDocRef.get().await()
            if (!targetDoc.exists()) return@withContext Result.failure(Exception("Target user not found"))

            val now = System.currentTimeMillis()
            val expiresAt = if (durationMs != null && durationMs > 0) now + durationMs else null

            val userUpdates = mutableMapOf<String, Any>()
            var notificationTitle = "Moderation Notice"
            var notificationBody = "An action was taken on your account."

            when (action) {
                ModerationActionTypes.WARN -> {
                    userUpdates["moderationStatus"] = "WARNED"
                    notificationTitle = "Account Warning"
                    notificationBody = "Your account has received an official warning for: $reason"
                }
                ModerationActionTypes.MUTE -> {
                    userUpdates["isMuted"] = true
                    userUpdates["mutedUntil"] = expiresAt ?: (now + 24 * 3600 * 1000)
                    userUpdates["moderationStatus"] = "RESTRICTED"
                    notificationTitle = "Account Muted"
                    notificationBody = "Your microphone and messaging permissions have been muted for: $reason"
                }
                ModerationActionTypes.RESTRICT -> {
                    userUpdates["moderationStatus"] = "RESTRICTED"
                    userUpdates["restrictedUntil"] = expiresAt ?: (now + 7 * 86400 * 1000)
                    notificationTitle = "Account Restricted"
                    notificationBody = "Your account features have been temporarily restricted for: $reason"
                }
                ModerationActionTypes.SUSPEND -> {
                    userUpdates["accountStatus"] = "SUSPENDED"
                    userUpdates["suspendedUntil"] = expiresAt ?: (now + 7 * 86400 * 1000)
                    notificationTitle = "Account Suspended"
                    notificationBody = "Your account has been temporarily suspended for: $reason"
                }
                ModerationActionTypes.BAN -> {
                    userUpdates["accountStatus"] = "BANNED"
                    userUpdates["suspendedUntil"] = Long.MAX_VALUE
                    notificationTitle = "Account Banned"
                    notificationBody = "Your account has been permanently banned for: $reason"
                }
                ModerationActionTypes.UNBAN -> {
                    userUpdates["accountStatus"] = "ACTIVE"
                    userUpdates["moderationStatus"] = "NORMAL"
                    userUpdates["isMuted"] = false
                    userUpdates["restrictedUntil"] = FieldValue.delete()
                    userUpdates["suspendedUntil"] = FieldValue.delete()
                    userUpdates["mutedUntil"] = FieldValue.delete()
                    notificationTitle = "Account Restored"
                    notificationBody = "Your account sanctions have been lifted. Thank you for your cooperation."
                }
            }

            // Apply updates to target user
            targetDocRef.update(userUpdates).await()

            // Write append-only audit log
            val logId = "log_${UUID.randomUUID()}"
            val auditLog = ModerationAuditLog(
                logId = logId,
                actorUid = actorUid,
                actorRole = actorRole,
                actorDisplayName = actorDisplayName,
                action = action,
                targetUid = targetUid,
                targetType = "USER",
                targetId = targetUid,
                reason = reason,
                duration = durationMs,
                reportId = reportId,
                createdAt = now
            )
            firestore.collection(MODERATION_LOGS_COLLECTION).document(logId).set(auditLog).await()

            // Send notification to the sanctioned user
            notificationRepository.sendNotification(
                AppNotification(
                    recipientUid = targetUid,
                    senderUid = actorUid,
                    type = NotificationTypes.MODERATION,
                    title = notificationTitle,
                    body = notificationBody,
                    referenceId = logId,
                    referenceType = "REPORT"
                )
            )

            // If action is associated with a report, update report status
            if (!reportId.isNullOrBlank()) {
                updateReportStatus(reportId, actorUid, "ACTION_TAKEN", "Action $action applied: $reason")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing moderation action: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of moderation audit logs.
     */
    fun getModerationAuditLogsStream(limit: Long = 50): Flow<List<ModerationAuditLog>> = callbackFlow {
        if (auth.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = firestore.collection(MODERATION_LOGS_COLLECTION)
            .limit(limit)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Notice listening to audit logs: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val logs = snapshot?.documents?.mapNotNull { it.toObject(ModerationAuditLog::class.java) } ?: emptyList()
            trySend(logs.sortedByDescending { it.createdAt })
        }

        awaitClose { registration.remove() }
    }

    // ==========================================
    // APPEALS SYSTEM
    // ==========================================

    suspend fun submitAppeal(
        userUid: String,
        actionId: String,
        reason: String,
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userDoc = firestore.collection(USERS_COLLECTION).document(userUid).get().await()
            val profile = userDoc.toObject(FirebaseUserProfile::class.java)

            val appealId = "appeal_${UUID.randomUUID()}"
            val appeal = AppealRecord(
                appealId = appealId,
                userUid = userUid,
                userPublicId = profile?.publicUserId ?: "",
                userDisplayName = profile?.displayName ?: "User",
                actionId = actionId,
                reason = reason,
                message = message.take(500),
                status = "PENDING",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection(APPEALS_COLLECTION).document(appealId).set(appeal).await()
            Result.success(appealId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAppealsStream(): Flow<List<AppealRecord>> = callbackFlow {
        if (auth.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = firestore.collection(APPEALS_COLLECTION).limit(50)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(AppealRecord::class.java) } ?: emptyList()
            trySend(list.sortedByDescending { it.createdAt })
        }

        awaitClose { registration.remove() }
    }

    suspend fun resolveAppeal(
        appealId: String,
        adminUid: String,
        decision: String, // "ACCEPTED" or "REJECTED"
        reviewNotes: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection(APPEALS_COLLECTION).document(appealId)
            val doc = docRef.get().await()
            if (!doc.exists()) return@withContext Result.failure(Exception("Appeal not found"))

            val appeal = doc.toObject(AppealRecord::class.java) ?: return@withContext Result.failure(Exception("Invalid appeal"))

            docRef.update(
                mapOf(
                    "status" to decision,
                    "reviewNotes" to reviewNotes,
                    "reviewedByUid" to adminUid,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            if (decision == "ACCEPTED") {
                // Lift sanctions on user
                executeModerationAction(
                    actorUid = adminUid,
                    actorRole = ModerationRoles.ADMIN,
                    actorDisplayName = "Admin",
                    targetUid = appeal.userUid,
                    action = ModerationActionTypes.UNBAN,
                    reason = "Appeal Accepted: $reviewNotes"
                )
            } else {
                // Notify user of rejection
                notificationRepository.sendNotification(
                    AppNotification(
                        recipientUid = appeal.userUid,
                        senderUid = adminUid,
                        type = NotificationTypes.MODERATION,
                        title = "Appeal Decision",
                        body = "Your account appeal has been reviewed: $decision ($reviewNotes)",
                        referenceId = appealId,
                        referenceType = "REPORT"
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
