package com.live.notesapp.presentation.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.utils.getSupabaseErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun signUp(name: String, email: String, password: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Full name cannot be empty") }
            return
        }
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Email cannot be empty") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(error = "Password cannot be empty") }
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(error = "Invalid email format") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Password must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.signUp(name, email, password)
            result.onSuccess {
                authRepository.syncProfile()
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.getSupabaseErrorMessage()) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}