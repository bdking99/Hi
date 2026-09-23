package com.example.ui.screens.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.NotificationPreferences
import com.example.data.repository.AuthRepository
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _preferences = MutableStateFlow(NotificationPreferences())
    val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

    private val _selectedFilter = MutableStateFlow("ALL") // "ALL", "SOCIAL", "GIFTS", "SYSTEM"
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var notificationsJob: Job? = null
    private var unreadJob: Job? = null
    private var prefsJob: Job? = null

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            authRepository.currentUserIdFlow.collectLatest { uid ->
                if (!uid.isNullOrBlank()) {
                    startObserving(uid)
                } else {
                    _notifications.value = emptyList()
                    _unreadCount.value = 0
                }
            }
        }
    }

    private fun startObserving(uid: String) {
        notificationsJob?.cancel()
        unreadJob?.cancel()
        prefsJob?.cancel()

        _isLoading.value = true

        notificationsJob = viewModelScope.launch {
            notificationRepository.getNotificationsStream(uid).collect { list ->
                _notifications.value = list
                _isLoading.value = false
            }
        }

        unreadJob = viewModelScope.launch {
            notificationRepository.getUnreadCountStream(uid).collect { count ->
                _unreadCount.value = count
            }
        }

        prefsJob = viewModelScope.launch {
            notificationRepository.getNotificationPreferencesStream(uid).collect { prefs ->
                _preferences.value = prefs
            }
        }
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun markAsRead(notificationId: String) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId, uid)
        }
    }

    fun markAllAsRead() {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            notificationRepository.markAllAsRead(uid)
        }
    }

    fun deleteNotification(notificationId: String) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId, uid)
        }
    }

    fun updatePreferences(newPrefs: NotificationPreferences) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _preferences.value = newPrefs
            notificationRepository.updateNotificationPreferences(uid, newPrefs)
        }
    }
}
