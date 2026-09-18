package com.example.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.ProfileEntity
import com.example.data.model.FirebaseUserProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ProfileUiState<out T> {
    object Idle : ProfileUiState<Nothing>()
    object Loading : ProfileUiState<Nothing>()
    data class Success<T>(val data: T) : ProfileUiState<T>()
    data class Error(val message: String) : ProfileUiState<Nothing>()
}

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _user = MutableStateFlow<UserEntity?>(null)
    val user: StateFlow<UserEntity?> = _user.asStateFlow()

    private val _profile = MutableStateFlow<ProfileEntity?>(null)
    val profile: StateFlow<ProfileEntity?> = _profile.asStateFlow()

    // Public Profile state (from Firestore) for current or viewed users
    private val _publicProfileState = MutableStateFlow<ProfileUiState<FirebaseUserProfile>>(ProfileUiState.Loading)
    val publicProfileState: StateFlow<ProfileUiState<FirebaseUserProfile>> = _publicProfileState.asStateFlow()

    private val _viewedUserProfile = MutableStateFlow<ProfileUiState<FirebaseUserProfile>>(ProfileUiState.Idle)
    val viewedUserProfile: StateFlow<ProfileUiState<FirebaseUserProfile>> = _viewedUserProfile.asStateFlow()

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            val token = authRepository.currentSessionToken.firstOrNull() ?: return@launch
            val session = authRepository.getCurrentSession(token) ?: return@launch
            
            userRepository.getUserFlow(session.userId).collect {
                _user.value = it
            }
        }
        viewModelScope.launch {
            val token = authRepository.currentSessionToken.firstOrNull() ?: return@launch
            val session = authRepository.getCurrentSession(token) ?: return@launch
            
            userRepository.getProfileFlow(session.userId).collect {
                _profile.value = it
            }
        }
        viewModelScope.launch {
            val token = authRepository.currentSessionToken.firstOrNull() ?: return@launch
            val session = authRepository.getCurrentSession(token) ?: return@launch

            // Fetch public profile stream from Firestore
            userRepository.getPublicProfileStream(session.userId).collect { firestoreProfile ->
                if (firestoreProfile != null) {
                    _publicProfileState.value = ProfileUiState.Success(firestoreProfile)
                } else {
                    _publicProfileState.value = ProfileUiState.Error("Profile not found")
                }
            }
        }
    }

    /**
     * Query public profile data from Firestore for any given user ID with state management
     */
    fun fetchPublicUserProfile(userId: String) {
        _viewedUserProfile.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                userRepository.getPublicProfileStream(userId).collect { profile ->
                    if (profile != null) {
                        _viewedUserProfile.value = ProfileUiState.Success(profile)
                    } else {
                        _viewedUserProfile.value = ProfileUiState.Error("User profile not found")
                    }
                }
            } catch (e: Exception) {
                _viewedUserProfile.value = ProfileUiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogoutComplete()
        }
    }

    fun updateProfile(displayName: String, bio: String, avatar: String?, coverImage: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            val userVal = _user.value ?: return@launch
            userRepository.updateProfile(userVal.id, displayName, bio, avatar, coverImage)
            onComplete()
        }
    }
}
