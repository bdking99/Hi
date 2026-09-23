package com.example.ui.screens.vip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.VipRepository
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VipViewModel(
    private val vipRepository: VipRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _userProfile = MutableStateFlow<FirebaseUserProfile?>(null)
    val userProfile: StateFlow<FirebaseUserProfile?> = _userProfile.asStateFlow()

    private val _vipLevels = MutableStateFlow<List<VipLevelConfig>>(emptyList())
    val vipLevels: StateFlow<List<VipLevelConfig>> = _vipLevels.asStateFlow()

    private val _userVipStatus = MutableStateFlow(UserVipStatus())
    val userVipStatus: StateFlow<UserVipStatus> = _userVipStatus.asStateFlow()

    private val _userLevelInfo = MutableStateFlow(UserLevelInfo())
    val userLevelInfo: StateFlow<UserLevelInfo> = _userLevelInfo.asStateFlow()

    private val _vipHistory = MutableStateFlow<List<VipHistoryItem>>(emptyList())
    val vipHistory: StateFlow<List<VipHistoryItem>> = _vipHistory.asStateFlow()

    private val _animationPrefs = MutableStateFlow(UserAnimationPreferences())
    val animationPrefs: StateFlow<UserAnimationPreferences> = _animationPrefs.asStateFlow()

    private val _activeTab = MutableStateFlow("VIP") // "VIP", "SVIP", "LEVEL", "HISTORY"
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _selectedTier = MutableStateFlow<VipLevelConfig?>(null)
    val selectedTier: StateFlow<VipLevelConfig?> = _selectedTier.asStateFlow()

    private val _showCelebration = MutableStateFlow<LevelUpRewardRecord?>(null)
    val showCelebration: StateFlow<LevelUpRewardRecord?> = _showCelebration.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            authRepository.currentUserIdFlow.collect { uid ->
                if (!uid.isNullOrBlank()) {
                    _currentUserId.value = uid
                    observeUserData(uid)
                }
            }
        }

        viewModelScope.launch {
            vipRepository.getVipLevelsStream().collect { levels ->
                _vipLevels.value = levels
                if (_selectedTier.value == null && levels.isNotEmpty()) {
                    _selectedTier.value = levels.firstOrNull { it.type == "VIP" && it.level == 1 } ?: levels.first()
                }
            }
        }

        viewModelScope.launch {
            vipRepository.animationPreferencesFlow.collect { prefs ->
                _animationPrefs.value = prefs
            }
        }
    }

    private fun observeUserData(uid: String) {
        viewModelScope.launch {
            userRepository.getPublicProfileStream(uid).collect { profile ->
                _userProfile.value = profile
            }
        }

        viewModelScope.launch {
            vipRepository.getUserVipStatusStream(uid).collect { status ->
                _userVipStatus.value = status
            }
        }

        viewModelScope.launch {
            vipRepository.getUserLevelStream(uid).collect { levelInfo ->
                _userLevelInfo.value = levelInfo
            }
        }

        viewModelScope.launch {
            vipRepository.getVipHistoryStream(uid).collect { history ->
                _vipHistory.value = history
            }
        }

        // Trigger authoritative backend recalculation
        syncAuthoritativeVipStatus()
    }

    fun setTab(tab: String) {
        _activeTab.value = tab
        val firstInTab = _vipLevels.value.firstOrNull { it.type == tab }
        if (firstInTab != null) {
            _selectedTier.value = firstInTab
        }
    }

    fun selectTier(config: VipLevelConfig) {
        _selectedTier.value = config
    }

    fun syncAuthoritativeVipStatus() {
        val uid = _currentUserId.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updatedStatus = vipRepository.calculateAndSyncVipLevel(uid)
                _userVipStatus.value = updatedStatus
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun awardXp(eventType: String, amount: Long) {
        val uid = _currentUserId.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            vipRepository.awardXp(uid, eventType, amount)
        }
    }

    fun equipVipFrame(frameId: String) {
        val uid = _currentUserId.value
        if (uid.isBlank() || frameId.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = walletRepository.equipFrame(uid, frameId)
            _isLoading.value = false
            if (result.isSuccess) {
                _snackbarMessage.value = "👑 VIP Frame equipped successfully!"
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.message ?: "Unable to equip frame"
            }
        }
    }

    fun updatePreferences(showVip: Boolean, showEntry: Boolean, showGift: Boolean, reduceMotion: Boolean) {
        viewModelScope.launch {
            vipRepository.updateAnimationPreferences(showVip, showEntry, showGift, reduceMotion)
            _snackbarMessage.value = "Preferences saved"
        }
    }

    fun adminUpdateThreshold(levelId: String, newThreshold: Long, benefits: List<String>, isActive: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = vipRepository.adminUpdateVipLevelThreshold(levelId, newThreshold, benefits, isActive)
            _isLoading.value = false
            if (success) {
                _snackbarMessage.value = "Threshold updated successfully"
                syncAuthoritativeVipStatus()
            } else {
                _snackbarMessage.value = "Failed to update threshold"
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun dismissCelebration() {
        _showCelebration.value = null
    }
}
