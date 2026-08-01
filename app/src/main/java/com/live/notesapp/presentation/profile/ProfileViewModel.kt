package com.live.notesapp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.domain.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeUser()
    }

    private fun observeUser() {
        _uiState.update { it.copy(isLoading = true) }
        authRepository.currentUser
            .onEach { user ->
                if (user != null) {
                    val name = user.userMetadata?.get("display_name")?.jsonPrimitive?.content
                        ?: user.userMetadata?.get("full_name")?.jsonPrimitive?.content
                        ?: "No Name"
                    val email = user.email ?: "No Email"
                    val userId = user.id
                    
                    _uiState.update {
                        it.copy(
                            name = name,
                            email = email,
                            userId = userId,
                            isLoading = false
                        )
                    }
                    syncProfile()
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun syncProfile() {
        viewModelScope.launch {
            authRepository.syncProfile()
        }
    }

    fun showLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun hideLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            themeRepository.clear()
            hideLogoutDialog()
            onLogout()
        }
    }

    fun verifyUser(uid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, scanResult = null) }
            val result = authRepository.checkUserExists(uid)
            result.onSuccess { exists ->
                if (exists) {
                    _uiState.update { it.copy(isLoading = false, scanResult = "User Found successful: $uid") }
                } else {
                    _uiState.update { it.copy(isLoading = false, scanResult = "User not found") }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, scanResult = "Error verifying user") }
            }
        }
    }

    fun clearScanResult() {
        _uiState.update { it.copy(scanResult = null) }
    }
}
