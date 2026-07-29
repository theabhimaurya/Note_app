package com.live.notesapp.presentation.signup

data class SignupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)