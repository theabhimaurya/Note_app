package com.live.notesapp.domain.manager

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.domain.repository.CallRepository
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val callRepository: CallRepository,
    private val authRepository: AuthRepository,
    private val auth: Auth
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Automatically sync FCM token whenever auth session becomes active
        scope.launch {
            authRepository.currentUser.collectLatest { user ->
                if (user != null) {
                    Log.d("FCM_TOKEN", "Auth session active for user ${user.id}, triggering token sync...")
                    syncCurrentToken()
                }
            }
        }
    }

    fun syncCurrentToken() {
        scope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                val userId = authRepository.getCurrentUserId() ?: auth.currentUserOrNull()?.id
                Log.d("FCM_TOKEN", "Current FCM token: $token, current userId: $userId")
                if (!userId.isNullOrBlank() && token.isNotBlank()) {
                    Log.d("FCM_TOKEN", "Syncing FCM token for user $userId to Supabase users table...")
                    callRepository.updateFcmToken(userId, token)
                } else {
                    Log.w("FCM_TOKEN", "Cannot sync token yet (userId=$userId, token=${token.take(8)}...)")
                }
            } catch (e: Exception) {
                Log.e("FCM_TOKEN", "Error syncing FCM token", e)
            }
        }
    }

    fun handleNewToken(newToken: String) {
        scope.launch {
            try {
                val userId = authRepository.getCurrentUserId() ?: auth.currentUserOrNull()?.id
                if (!userId.isNullOrBlank() && newToken.isNotBlank()) {
                    Log.d("FCM_TOKEN", "Updating new FCM token for user $userId to Supabase users table: $newToken")
                    callRepository.updateFcmToken(userId, newToken)
                }
            } catch (e: Exception) {
                Log.e("FCM_TOKEN", "Error handling new FCM token", e)
            }
        }
    }
}
