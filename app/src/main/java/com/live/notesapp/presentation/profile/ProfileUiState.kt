package com.live.notesapp.presentation.profile

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val userId: String = "",
    val profileImageUrl: String? = null,
    val isLoading: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val scanResult: String? = null,
    val isScanning: Boolean = false
)
