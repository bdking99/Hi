package com.example.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.ModerationRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminUiState(
    val currentUserProfile: FirebaseUserProfile? = null,
    val metrics: SystemMetrics = SystemMetrics(),
    val featureFlags: List<FeatureFlag> = emptyList(),
    val systemSettings: SystemSettingsRecord = SystemSettingsRecord(),
    val userSearchResults: List<FirebaseUserProfile> = emptyList(),
    val giftsCatalog: List<CatalogGift> = emptyList(),
    val framesCatalog: List<ProfileFrameItem> = emptyList(),
    val auditLogs: List<AdminAuditLogRecord> = emptyList(),
    val pendingPurchaseRequests: List<CoinPurchaseRequest> = emptyList(),
    val pendingWithdrawalRequests: List<WithdrawalRequest> = emptyList(),
    val paymentSystemConfig: PaymentSystemConfig = PaymentSystemConfig(),
    val selectedTab: AdminTab = AdminTab.OVERVIEW,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

enum class AdminTab(val title: String) {
    OVERVIEW("Overview"),
    USERS("Users"),
    WALLET("Coins & Wallets"),
    GIFTS("Gifts"),
    FRAMES("Frames"),
    FLAGS("Feature Flags"),
    SETTINGS("Settings"),
    AUDIT("Audit Logs")
}

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val moderationRepository: ModerationRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeDataStreams()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentUid = authRepository.currentFirebaseUid
            if (!currentUid.isNullOrBlank()) {
                val profile = userRepository.getUserProfile(currentUid)
                _uiState.update { it.copy(currentUserProfile = profile) }
            }
            refreshMetrics()
            searchUsers("")
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun observeDataStreams() {
        viewModelScope.launch {
            authRepository.currentUserIdFlow.collectLatest { uid ->
                if (!uid.isNullOrBlank()) {
                    userRepository.getPublicProfileStream(uid).collectLatest { profile ->
                        _uiState.update { it.copy(currentUserProfile = profile) }
                    }
                }
            }
        }
        viewModelScope.launch {
            adminRepository.getFeatureFlagsStream().collect { flags ->
                _uiState.update { it.copy(featureFlags = flags) }
            }
        }

        viewModelScope.launch {
            adminRepository.getSystemSettingsStream().collect { settings ->
                _uiState.update { it.copy(systemSettings = settings) }
            }
        }

        viewModelScope.launch {
            walletRepository.getGiftsCatalogStream().collect { gifts ->
                _uiState.update { it.copy(giftsCatalog = gifts) }
            }
        }

        viewModelScope.launch {
            walletRepository.getProfileFramesCatalogStream().collect { frames ->
                _uiState.update { it.copy(framesCatalog = frames) }
            }
        }

        viewModelScope.launch {
            adminRepository.getAllAuditLogsStream().collect { logs ->
                _uiState.update { it.copy(auditLogs = logs) }
            }
        }

        viewModelScope.launch {
            walletRepository.getPendingCoinPurchaseRequestsStream().collect { requests ->
                _uiState.update { it.copy(pendingPurchaseRequests = requests) }
            }
        }

        viewModelScope.launch {
            walletRepository.getPendingWithdrawalRequestsStream().collect { requests ->
                _uiState.update { it.copy(pendingWithdrawalRequests = requests) }
            }
        }

        viewModelScope.launch {
            walletRepository.getPaymentSystemConfigStream().collect { config ->
                _uiState.update { it.copy(paymentSystemConfig = config) }
            }
        }
    }

    fun selectTab(tab: AdminTab) {
        _uiState.update { it.copy(selectedTab = tab, errorMessage = null, successMessage = null) }
        if (tab == AdminTab.OVERVIEW) {
            refreshMetrics()
        }
    }

    fun refreshMetrics() {
        viewModelScope.launch {
            adminRepository.fetchSystemMetrics().onSuccess { metrics ->
                _uiState.update { it.copy(metrics = metrics) }
            }
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(searchQuery = query) }
            adminRepository.searchUsers(query).onSuccess { users ->
                _uiState.update { it.copy(userSearchResults = users) }
            }
        }
    }

    fun toggleFeatureFlag(featureId: String, enabled: Boolean) {
        val user = _uiState.value.currentUserProfile ?: return
        viewModelScope.launch {
            adminRepository.toggleFeatureFlag(
                featureId = featureId,
                enabled = enabled,
                adminUid = user.uid,
                adminRole = user.role,
                adminDisplayName = user.displayName
            ).onSuccess {
                _uiState.update { it.copy(successMessage = "Feature flag updated successfully.") }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to toggle feature flag.") }
            }
        }
    }

    fun updateSystemSettings(settings: SystemSettingsRecord) {
        val user = _uiState.value.currentUserProfile ?: return
        viewModelScope.launch {
            adminRepository.updateSystemSettings(
                settings = settings,
                adminUid = user.uid,
                adminRole = user.role,
                adminDisplayName = user.displayName
            ).onSuccess {
                _uiState.update { it.copy(successMessage = "System settings updated successfully.") }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to update settings.") }
            }
        }
    }

    fun updateUserRole(targetUid: String, newRole: String, reason: String) {
        val user = _uiState.value.currentUserProfile ?: return
        viewModelScope.launch {
            adminRepository.updateUserRole(
                adminUid = user.uid,
                adminRole = user.role,
                adminDisplayName = user.displayName,
                targetUid = targetUid,
                newRole = newRole,
                reason = reason
            ).onSuccess {
                _uiState.update { it.copy(successMessage = "User role successfully changed to $newRole.") }
                searchUsers(_uiState.value.searchQuery)
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to update user role.") }
            }
        }
    }

    fun adjustUserCoins(targetUid: String, targetPublicId: String, delta: Long, reason: String) {
        val user = _uiState.value.currentUserProfile ?: return
        viewModelScope.launch {
            adminRepository.adjustUserCoins(
                adminUid = user.uid,
                adminRole = user.role,
                adminDisplayName = user.displayName,
                targetUid = targetUid,
                targetPublicId = targetPublicId,
                amountDelta = delta,
                reason = reason
            ).onSuccess { newBal ->
                _uiState.update { it.copy(successMessage = "Wallet balance updated. New Balance: $newBal coins.") }
                searchUsers(_uiState.value.searchQuery)
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to adjust coins.") }
            }
        }
    }

    fun saveGift(gift: CatalogGift) {
        val user = _uiState.value.currentUserProfile ?: return
        viewModelScope.launch {
            adminRepository.saveGift(
                adminUid = user.uid,
                adminRole = user.role,
                adminDisplayName = user.displayName,
                gift = gift
            ).onSuccess {
                _uiState.update { it.copy(successMessage = "Gift catalog entry saved.") }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to save gift.") }
            }
        }
    }

    fun saveProfileFrame(frame: ProfileFrameItem) {
        val user = _uiState.value.currentUserProfile ?: return
        viewModelScope.launch {
            adminRepository.saveProfileFrame(
                adminUid = user.uid,
                adminRole = user.role,
                adminDisplayName = user.displayName,
                frame = frame
            ).onSuccess {
                _uiState.update { it.copy(successMessage = "Profile frame entry saved.") }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to save frame.") }
            }
        }
    }

    fun executeUserSanction(targetUid: String, action: String, reason: String, durationMs: Long?) {
        val user = _uiState.value.currentUserProfile ?: return
        viewModelScope.launch {
            moderationRepository.executeModerationAction(
                actorUid = user.uid,
                actorRole = user.role,
                actorDisplayName = user.displayName,
                targetUid = targetUid,
                action = action,
                reason = reason,
                durationMs = durationMs
            ).onSuccess {
                _uiState.update { it.copy(successMessage = "Sanction ($action) applied successfully.") }
                searchUsers(_uiState.value.searchQuery)
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to apply sanction.") }
            }
        }
    }

    fun approveCoinPurchase(requestId: String) {
        val user = _uiState.value.currentUserProfile ?: return
        viewModelScope.launch {
            walletRepository.approveCoinPurchaseRequest(
                adminUid = user.uid,
                adminRole = user.role,
                adminDisplayName = user.displayName,
                requestId = requestId
            ).onSuccess {
                _uiState.update { it.copy(successMessage = "Coin Purchase Request approved and user credited!") }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to approve purchase request.") }
            }
        }
    }

    fun rejectCoinPurchase(requestId: String, reason: String) {
        val user = _uiState.value.currentUserProfile ?: return
        viewModelScope.launch {
            walletRepository.rejectCoinPurchaseRequest(
                requestId = requestId,
                rejectionReason = reason,
                adminUid = user.uid
            ).onSuccess {
                _uiState.update { it.copy(successMessage = "Coin Purchase Request rejected.") }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to reject purchase request.") }
            }
        }
    }

    fun reviewWithdrawal(requestId: String, approved: Boolean, rejectionReason: String?, payoutTrxId: String?) {
        val user = _uiState.value.currentUserProfile ?: return
        viewModelScope.launch {
            walletRepository.reviewWithdrawalRequest(
                adminUid = user.uid,
                adminRole = user.role,
                adminDisplayName = user.displayName,
                requestId = requestId,
                approve = approved,
                transactionRef = payoutTrxId,
                rejectReason = rejectionReason
            ).onSuccess {
                _uiState.update {
                    it.copy(successMessage = if (approved) "Withdrawal approved and completed." else "Withdrawal rejected.")
                }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to process withdrawal request.") }
            }
        }
    }

    fun updatePaymentSystemConfig(config: PaymentSystemConfig) {
        viewModelScope.launch {
            walletRepository.updatePaymentSystemConfig(config).onSuccess {
                _uiState.update { it.copy(successMessage = "Payment configuration updated successfully.") }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to update payment config.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
