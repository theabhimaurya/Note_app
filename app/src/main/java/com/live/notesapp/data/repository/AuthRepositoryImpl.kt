package com.live.notesapp.data.repository

import com.live.notesapp.data.local.SessionManager
import com.live.notesapp.domain.model.ChatUser
import com.live.notesapp.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val sessionManager: SessionManager
) : AuthRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Keep session manager updated whenever Supabase auth status changes
        scope.launch {
            auth.sessionStatus.collectLatest { status ->
                if (status is SessionStatus.Authenticated) {
                    val user = status.session.user
                    val name = user?.userMetadata?.get("display_name")?.jsonPrimitive?.content
                        ?: user?.userMetadata?.get("full_name")?.jsonPrimitive?.content
                        ?: user?.userMetadata?.get("name")?.jsonPrimitive?.content
                    val token = status.session.accessToken
                    if (user != null) {
                        sessionManager.saveSession(
                            userId = user.id,
                            email = user.email ?: "",
                            displayName = name,
                            authToken = token
                        )
                    }
                } else if (status is SessionStatus.NotAuthenticated) {
                    // Do not clear immediately on temporary network disconnects, only on explicit logout
                }
            }
        }
    }

    override val currentUser: Flow<UserInfo?> = auth.sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)?.session?.user
    }

    override fun getCurrentUserId(): String? = auth.currentUserOrNull()?.id ?: sessionManager.getUserId()

    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("display_name", name)
                    put("full_name", name)
                }
            }
            val user = auth.currentUserOrNull()
            if (user != null) {
                sessionManager.saveSession(
                    userId = user.id,
                    email = user.email ?: email,
                    displayName = name,
                    authToken = auth.currentAccessTokenOrNull()
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val user = auth.currentUserOrNull()
            val name = user?.userMetadata?.get("display_name")?.jsonPrimitive?.content
                ?: user?.userMetadata?.get("full_name")?.jsonPrimitive?.content
                ?: user?.userMetadata?.get("name")?.jsonPrimitive?.content
            if (user != null) {
                sessionManager.saveSession(
                    userId = user.id,
                    email = user.email ?: email,
                    displayName = name,
                    authToken = auth.currentAccessTokenOrNull()
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            try {
                auth.signOut()
            } catch (e: Exception) {
                // Ignore network error during signOut
            }
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Result.failure(e)
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return sessionManager.isLoggedIn() || (auth.currentUserOrNull() != null)
    }

    override suspend fun checkUserExists(userId: String): Result<Boolean> {
        return try {
            val response = postgrest["profiles"].select {
                filter {
                    eq("id", userId)
                }
            }
            val exists = response.decodeList<ChatUser>().isNotEmpty()
            Result.success(exists)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun syncProfile(): Result<Unit> {
        return try {
            val user = auth.currentUserOrNull() ?: return Result.failure(Exception("Not logged in"))
            
            // Try to get existing profile
            val profile = try {
                postgrest["profiles"].select {
                    filter { eq("id", user.id) }
                }.decodeSingleOrNull<ChatUser>()
            } catch (e: Exception) {
                null
            }

            val name = user.userMetadata?.get("display_name")?.jsonPrimitive?.content
                ?: user.userMetadata?.get("full_name")?.jsonPrimitive?.content
                ?: user.userMetadata?.get("name")?.jsonPrimitive?.content
                ?: "No Name"

            if (profile == null) {
                val newProfile = buildJsonObject {
                    put("id", user.id)
                    put("display_name", name)
                    put("email", user.email ?: "")
                }
                try {
                    postgrest["profiles"].insert(newProfile)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Also ensure row in public.users exists
            try {
                val userRecord = buildJsonObject {
                    put("id", user.id)
                    put("name", name)
                    put("email", user.email ?: "")
                }
                postgrest["users"].upsert(userRecord)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
