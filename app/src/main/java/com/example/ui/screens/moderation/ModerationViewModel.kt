package com.example.ui.screens.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.ModerationRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ModerationViewModel(
    private val moderationRepository: ModerationRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _myReports = MutableStateFlow<List<ReportRecord>>(emptyList())
    val myReports: StateFlow<List<ReportRecord>> = _myReports.asStateFlow()

    private val _allReports = MutableStateFlow<List<ReportRecord>>(emptyList())
    val allReports: StateFlow<List<ReportRecord>> = _allReports.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<ModerationAuditLog>>(emptyList())
    val auditLogs: StateFlow<List<ModerationAuditLog>> = _auditLogs.asStateFlow()

    private val _appeals = MutableStateFlow<List<AppealRecord>>(emptyList())
    val appeals: StateFlow<List<AppealRecord>> = _appeals.asStateFlow()

    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var myReportsJob: Job? = null
    private var allReportsJob: Job? = null
    private var auditJob: Job? = null
    private var appealsJob: Job? = null

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            authRepository.currentUserIdFlow.collectLatest { uid ->
                if (!uid.isNullOrBlank()) {
                    startObserving(uid)
                } else {
                    _myReports.value = emptyList()
                    _allReports.value = emptyList()
                    _auditLogs.value = emptyList()
                    _appeals.value = emptyList()
                }
            }
        }
    }

    private fun startObserving(uid: String) {
        myReportsJob?.cancel()
        allReportsJob?.cancel()
        auditJob?.cancel()
        appealsJob?.cancel()

        myReportsJob = viewModelScope.launch {
            moderationRepository.getMyReportsStream(uid).collect { list ->
                _myReports.value = list
            }
        }

        allReportsJob = viewModelScope.launch {
            moderationRepository.getAllReportsStream().collect { list ->
                _allReports.value = list
            }
        }

        auditJob = viewModelScope.launch {
            moderationRepository.getModerationAuditLogsStream().collect { list ->
                _auditLogs.value = list
            }
        }

        appealsJob = viewModelScope.launch {
            moderationRepository.getAppealsStream().collect { list ->
                _appeals.value = list
            }
        }
    }

    fun submitReport(
        targetType: String,
        targetId: String,
        targetUid: String,
        targetDisplayName: String,
        reason: String,
        description: String,
        evidence: String = "",
        onSuccess: (() -> Unit)? = null
    ) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = moderationRepository.submitReport(
                reporterUid = uid,
                targetType = targetType,
                targetId = targetId,
                targetUid = targetUid,
                targetDisplayName = targetDisplayName,
                reason = reason,
                description = description,
                evidence = evidence
            )
            _isLoading.value = false
            result.onSuccess {
                _statusNotice.value = "Report submitted successfully. Our team will review it."
                onSuccess?.invoke()
            }.onFailure { e ->
                _statusNotice.value = e.message ?: "Failed to submit report."
            }
        }
    }

    fun executeModerationAction(
        targetUid: String,
        action: String,
        reason: String,
        durationMs: Long? = null,
        reportId: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val adminProfile = userRepository.getUserProfile(uid)
            val result = moderationRepository.executeModerationAction(
                actorUid = uid,
                actorRole = adminProfile?.role ?: ModerationRoles.MODERATOR,
                actorDisplayName = adminProfile?.displayName ?: "Moderator",
                targetUid = targetUid,
                action = action,
                reason = reason,
                durationMs = durationMs,
                reportId = reportId
            )
            _isLoading.value = false
            result.onSuccess {
                _statusNotice.value = "Moderation action '$action' executed successfully."
                onSuccess?.invoke()
            }.onFailure { e ->
                _statusNotice.value = e.message ?: "Failed to execute moderation action."
            }
        }
    }

    fun updateReportStatus(reportId: String, newStatus: String, notes: String) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            moderationRepository.updateReportStatus(reportId, uid, newStatus, notes)
        }
    }

    fun submitAppeal(actionId: String, reason: String, message: String, onSuccess: (() -> Unit)? = null) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = moderationRepository.submitAppeal(uid, actionId, reason, message)
            _isLoading.value = false
            result.onSuccess {
                _statusNotice.value = "Appeal submitted for review."
                onSuccess?.invoke()
            }.onFailure { e ->
                _statusNotice.value = e.message ?: "Failed to submit appeal."
            }
        }
    }

    fun resolveAppeal(appealId: String, decision: String, notes: String) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            moderationRepository.resolveAppeal(appealId, uid, decision, notes)
        }
    }

    fun clearNotice() {
        _statusNotice.value = null
    }
}
