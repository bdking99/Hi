package com.example.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _user = MutableStateFlow<UserEntity?>(null)
    val user: StateFlow<UserEntity?> = _user.asStateFlow()

    private val _profile = MutableStateFlow<ProfileEntity?>(null)
    val profile: StateFlow<ProfileEntity?> = _profile.asStateFlow()

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
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogoutComplete()
        }
    }
}
