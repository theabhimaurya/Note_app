package com.live.notesapp.data.repository

import com.live.notesapp.domain.model.ChatUser
import com.live.notesapp.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) : AuthRepository {

    override val currentUser: Flow<UserInfo?> = auth.sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)?.session?.user
    }

    override fun getCurrentUserId(): String? = auth.currentUserOrNull()?.id

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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isUserLoggedIn(): Boolean {
        return auth.currentUserOrNull() != null
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

            if (profile == null) {
                val name = user.userMetadata?.get("display_name")?.jsonPrimitive?.content
                    ?: user.userMetadata?.get("full_name")?.jsonPrimitive?.content
                    ?: "No Name"
                
                val newProfile = buildJsonObject {
                    put("id", user.id)
                    put("display_name", name)
                    put("email", user.email ?: "")
                }
                postgrest["profiles"].insert(newProfile)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
