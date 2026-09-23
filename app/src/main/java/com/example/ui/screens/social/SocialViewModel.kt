package com.example.ui.screens.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SocialViewModel(
    private val socialRepository: SocialRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _followers = MutableStateFlow<List<FollowerRecord>>(emptyList())
    val followers: StateFlow<List<FollowerRecord>> = _followers.asStateFlow()

    private val _following = MutableStateFlow<List<FollowingRecord>>(emptyList())
    val following: StateFlow<List<FollowingRecord>> = _following.asStateFlow()

    private val _followRequests = MutableStateFlow<List<FollowRequest>>(emptyList())
    val followRequests: StateFlow<List<FollowRequest>> = _followRequests.asStateFlow()

    private val _blockedUsers = MutableStateFlow<List<BlockedUserRecord>>(emptyList())
    val blockedUsers: StateFlow<List<BlockedUserRecord>> = _blockedUsers.asStateFlow()

    private val _actionStatus = MutableStateFlow<String?>(null)
    val actionStatus: StateFlow<String?> = _actionStatus.asStateFlow()

    private var followersJob: Job? = null
    private var followingJob: Job? = null
    private var requestsJob: Job? = null
    private var blockedJob: Job? = null

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            authRepository.currentUserIdFlow.collectLatest { uid ->
                if (!uid.isNullOrBlank()) {
                    startObserving(uid)
                } else {
                    _followers.value = emptyList()
                    _following.value = emptyList()
                    _followRequests.value = emptyList()
                    _blockedUsers.value = emptyList()
                }
            }
        }
    }

    private fun startObserving(uid: String) {
        followersJob?.cancel()
        followingJob?.cancel()
        requestsJob?.cancel()
        blockedJob?.cancel()

        followersJob = viewModelScope.launch {
            socialRepository.getFollowersStream(uid).collect { list ->
                _followers.value = list
            }
        }

        followingJob = viewModelScope.launch {
            socialRepository.getFollowingStream(uid).collect { list ->
                _following.value = list
            }
        }

        requestsJob = viewModelScope.launch {
            socialRepository.getPendingFollowRequestsStream(uid).collect { list ->
                _followRequests.value = list
            }
        }

        blockedJob = viewModelScope.launch {
            socialRepository.getBlockedUsersStream(uid).collect { list ->
                _blockedUsers.value = list
            }
        }
    }

    fun toggleFollow(targetUser: FirebaseUserProfile, onComplete: ((String) -> Unit)? = null) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = socialRepository.toggleFollow(uid, targetUser)
            result.onSuccess { status ->
                _actionStatus.value = if (status == "FOLLOWING") "Followed ${targetUser.displayName}" else "Unfollowed ${targetUser.displayName}"
                onComplete?.invoke(status)
            }.onFailure { e ->
                _actionStatus.value = e.message ?: "Failed to update follow"
            }
        }
    }

    fun respondToFollowRequest(requestId: String, accept: Boolean) {
        viewModelScope.launch {
            socialRepository.respondToFollowRequest(requestId, accept)
        }
    }

    fun blockUser(targetUser: FirebaseUserProfile, reason: String? = null, onComplete: (() -> Unit)? = null) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = socialRepository.blockUser(uid, targetUser, reason)
            result.onSuccess {
                _actionStatus.value = "${targetUser.displayName} has been blocked."
                onComplete?.invoke()
            }.onFailure { e ->
                _actionStatus.value = e.message ?: "Failed to block user."
            }
        }
    }

    fun unblockUser(targetUid: String, displayName: String = "User") {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = socialRepository.unblockUser(uid, targetUid)
            result.onSuccess {
                _actionStatus.value = "$displayName unblocked."
            }
        }
    }

    fun clearStatus() {
        _actionStatus.value = null
    }
}
