package com.live.notesapp.domain.repository

import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<UserInfo?>
    fun getCurrentUserId(): String?
    suspend fun signUp(name: String, email: String, password: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun isUserLoggedIn(): Boolean
    suspend fun checkUserExists(userId: String): Result<Boolean>
    suspend fun syncProfile(): Result<Unit>
}