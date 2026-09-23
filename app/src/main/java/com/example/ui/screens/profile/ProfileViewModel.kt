package com.example.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.ProfileEntity
import com.example.data.model.FirebaseUserProfile
import com.example.data.model.ProfileFrame
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ProfileUiState<out T> {
    object Idle : ProfileUiState<Nothing>()
    object Loading : ProfileUiState<Nothing>()
    data class Success<T>(val data: T) : ProfileUiState<T>()
    data class Error(val message: String) : ProfileUiState<Nothing>()
}

class ProfileViewModel(
    val authRepository: AuthRepository,
    val userRepository: UserRepository
) : ViewModel() {

    private val _user = MutableStateFlow<UserEntity?>(null)
    val user: StateFlow<UserEntity?> = _user.asStateFlow()

    private val _profile = MutableStateFlow<ProfileEntity?>(null)
    val profile: StateFlow<ProfileEntity?> = _profile.asStateFlow()

    // Public Profile state (from Firestore) for current user
    private val _publicProfileState = MutableStateFlow<ProfileUiState<FirebaseUserProfile>>(ProfileUiState.Loading)
    val publicProfileState: StateFlow<ProfileUiState<FirebaseUserProfile>> = _publicProfileState.asStateFlow()

    val availableFrames: StateFlow<List<ProfileFrame>> = userRepository.getAvailableFramesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), userRepository.getDefaultFrames())

    val currentUserId: String
        get() = authRepository.currentFirebaseUid ?: _user.value?.id ?: ""

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            authRepository.currentUserIdFlow.collect { storedUid ->
                val activeUid = storedUid ?: authRepository.currentFirebaseUid
                if (activeUid != null) {
                    // Update presence in Firestore
                    userRepository.updatePresence(activeUid, true)

                    // Collect local user
                    launch {
                        userRepository.getUserFlow(activeUid).collect {
                            _user.value = it
                        }
                    }
                    // Collect local profile
                    launch {
                        userRepository.getProfileFlow(activeUid).collect {
                            _profile.value = it
                        }
                    }
                    // Collect Firestore profile
                    launch {
                        userRepository.getPublicProfileStream(activeUid).collect { firestoreProfile ->
                            if (firestoreProfile != null) {
                                _publicProfileState.value = ProfileUiState.Success(firestoreProfile)
                            } else {
                                // If Firestore doc not yet created, build initial view from local room
                                val local = _user.value
                                if (local != null) {
                                    val fallback = FirebaseUserProfile(
                                        userId = local.id,
                                        publicUserId = local.publicUserId,
                                        displayName = local.displayName,
                                        username = local.username,
                                        email = local.email ?: "",
                                        avatar = local.avatar ?: "",
                                        isOnline = true
                                    )
                                    _publicProfileState.value = ProfileUiState.Success(fallback)
                                } else {
                                    _publicProfileState.value = ProfileUiState.Loading
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectProfileFrame(frameId: String) {
        viewModelScope.launch {
            val uid = currentUserId
            if (uid.isNotBlank()) {
                userRepository.updateProfileFrame(uid, frameId)
            }
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogoutComplete()
        }
    }

    fun updateProfile(
        displayName: String,
        bio: String,
        avatar: String?,
        coverImage: String?,
        country: String? = null,
        gender: String? = null,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val uid = currentUserId
            if (uid.isNotBlank()) {
                userRepository.updateProfile(uid, displayName, bio, avatar, coverImage, country, gender)
                onComplete()
            }
        }
    }
}

