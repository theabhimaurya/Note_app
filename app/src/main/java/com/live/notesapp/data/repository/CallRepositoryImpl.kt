package com.live.notesapp.data.repository

import android.util.Log
import com.live.notesapp.domain.model.CallSession
import com.live.notesapp.domain.repository.CallRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CallSessionDto(
    val id: String? = null,
    @SerialName("room_id") val roomId: String,
    @SerialName("caller_id") val callerId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("call_type") val callType: String,
    @SerialName("call_status") val callStatus: String
)

@Serializable
data class CallStatusUpdateDto(
    @SerialName("call_status") val callStatus: String
)

@Singleton
class CallRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth
) : CallRepository {

    override suspend fun createCallSession(
        callerId: String,
        receiverId: String,
        roomId: String,
        callType: String
    ): CallSession = withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
        val payload = buildJsonObject {
            put("room_id", roomId)
            put("caller_id", callerId)
            put("receiver_id", receiverId)
            put("call_type", callType)
            put("call_status", "ringing")
        }
        Log.d("CALL_SESSION", "Creating call session: caller=$callerId, receiver=$receiverId, room=$roomId")
        try {
            val response = postgrest["call_sessions"]
                .insert(payload) { select() }
                .decodeSingle<CallSessionDto>()

            Log.d("CALL_SESSION", "Call session created in DB successfully! ID=${response.id}")

            // Trigger Edge Function directly from app as well to guarantee instant notification
            try {
                val edgeFunctionUrl = java.net.URL("${com.live.notesapp.utils.Constants.SUPABASE_URL}/functions/v1/push-call-notification")
                val conn = (edgeFunctionUrl.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", com.live.notesapp.utils.Constants.SUPABASE_KEY)
                    val token = auth.currentAccessTokenOrNull() ?: com.live.notesapp.utils.Constants.SUPABASE_KEY
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                val reqBody = buildJsonObject {
                    put("caller_id", callerId)
                    put("receiver_id", receiverId)
                    put("room_id", roomId)
                    put("call_type", callType)
                    put("id", response.id ?: "")
                }.toString()
                conn.outputStream.use { os ->
                    os.write(reqBody.toByteArray(Charsets.UTF_8))
                }
                val code = conn.responseCode
                val responseText = if (code in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                }
                Log.d("CALL_SESSION", "Direct Edge Function trigger HTTP $code: $responseText")
            } catch (e: Exception) {
                Log.w("CALL_SESSION", "Direct Edge Function call notice: ${e.message}")
            }

            CallSession(
                id = response.id ?: "",
                roomId = response.roomId,
                callerId = response.callerId,
                receiverId = response.receiverId,
                callType = response.callType,
                status = response.callStatus
            )
        } catch (e: Exception) {
            Log.e("CALL_SESSION", "Error inserting into call_sessions: ${e.message}", e)
            throw e
        }
    }

    override suspend fun updateCallStatus(callId: String, status: String): Unit = withContext(Dispatchers.IO) {
        postgrest["call_sessions"]
            .update(CallStatusUpdateDto(callStatus = status)) {
                filter { eq("id", callId) }
            }
    }

    override suspend fun updateFcmToken(userId: String, token: String): Unit = withContext(Dispatchers.IO) {
        val now = java.time.Instant.now().toString()
        val currentUser = auth.currentUserOrNull()
        val name = currentUser?.userMetadata?.get("display_name")?.jsonPrimitive?.content
            ?: currentUser?.userMetadata?.get("full_name")?.jsonPrimitive?.content
            ?: currentUser?.userMetadata?.get("name")?.jsonPrimitive?.content
            ?: ""
        val email = currentUser?.email ?: ""

        val userPayload = buildJsonObject {
            put("id", userId)
            put("fcm_token", token)
            put("fcm_token_updated_at", now)
            put("updated_at", now)
            if (name.isNotBlank()) put("name", name)
            if (email.isNotBlank()) put("email", email)
        }

        try {
            Log.d("FCM_TOKEN", "Attempting upsert into users table for id=$userId, token=$token")
            postgrest["users"].upsert(userPayload)
            Log.d("FCM_TOKEN", "Successfully saved FCM token to users table via upsert!")
        } catch (e: Exception) {
            Log.e("FCM_TOKEN", "Upsert into users table failed: ${e.message}. Falling back to update...", e)
            try {
                val updatePayload = buildJsonObject {
                    put("fcm_token", token)
                    put("fcm_token_updated_at", now)
                    put("updated_at", now)
                }
                postgrest["users"].update(updatePayload) {
                    filter { eq("id", userId) }
                }
                Log.d("FCM_TOKEN", "Successfully updated FCM token in users table via update!")
            } catch (updateEx: Exception) {
                Log.e("FCM_TOKEN", "Update FCM token in users table also failed: ${updateEx.message}", updateEx)
                throw updateEx
            }
        }
    }

    override suspend fun getCallSession(callId: String): CallSession? = withContext(Dispatchers.IO) {
        try {
            val dto = postgrest["call_sessions"]
                .select { filter { eq("id", callId) } }
                .decodeSingleOrNull<CallSessionDto>() ?: return@withContext null

            CallSession(
                id = dto.id ?: "",
                roomId = dto.roomId,
                callerId = dto.callerId,
                receiverId = dto.receiverId,
                callType = dto.callType,
                status = dto.callStatus
            )
        } catch (e: Exception) {
            null
        }
    }
}
