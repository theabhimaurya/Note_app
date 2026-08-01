package com.live.notesapp.data.repository

import com.live.notesapp.domain.model.ChatMessage
import com.live.notesapp.domain.model.ChatRoom
import com.live.notesapp.domain.model.ChatUser
import com.live.notesapp.domain.model.CallSignal
import com.live.notesapp.domain.repository.ChatRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.broadcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val realtime: Realtime
) : ChatRepository {

    private val signalingChannels = mutableMapOf<String, io.github.jan.supabase.realtime.RealtimeChannel>()

    override suspend fun sendCallSignal(roomId: String, signal: CallSignal) {
        try {
            val channel = signalingChannels.getOrPut(roomId) {
                realtime.channel("signaling_$roomId")
            }
            if (channel.status.value != io.github.jan.supabase.realtime.RealtimeChannel.Status.SUBSCRIBED) {
                channel.subscribe()
            }
            channel.broadcast("signal", signal)
        } catch (e: Exception) {
            e.printStackTrace()
            // We catch but don't rethrow to avoid crashing the app on rate limits or network issues
            // Signaling is best-effort in this implementation
        }
    }

    override fun observeCallSignals(roomId: String): Flow<CallSignal> {
        val channel = signalingChannels.getOrPut(roomId) {
            realtime.channel("signaling_$roomId")
        }
        return channel.broadcastFlow<CallSignal>("signal")
            .onStart { 
                if (channel.status.value != io.github.jan.supabase.realtime.RealtimeChannel.Status.SUBSCRIBED) {
                    channel.subscribe()
                }
            }
    }

    override suspend fun createOrGetChatRoom(otherUserId: String): String {
        val currentUser = auth.currentUserOrNull() ?: throw Exception("User not logged in")
        val currentUserId = currentUser.id
        
        // Ensure our own profile exists. We can't ensure other's profiles due to RLS
        ensureProfileExists(currentUserId)
        
        // Try to find an existing room
        val existingRoom = try {
            postgrest["chat_rooms"].select {
                filter {
                    or {
                        and {
                            eq("participant1", currentUserId)
                            eq("participant2", otherUserId)
                        }
                        and {
                            eq("participant1", otherUserId)
                            eq("participant2", currentUserId)
                        }
                    }
                }
            }.decodeSingleOrNull<ChatRoom>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (existingRoom != null) {
            return existingRoom.id!!
        }

        // Create new room
        val newRoom = ChatRoom(
            participant1 = currentUserId,
            participant2 = otherUserId,
            lastMessage = "No messages yet",
            lastMessageTime = System.currentTimeMillis()
        )
        
        return try {
            val createdRoom = postgrest["chat_rooms"].insert(newRoom) {
                select()
            }.decodeSingle<ChatRoom>()
            createdRoom.id!!
        } catch (e: Exception) {
            e.printStackTrace()
            if (e.message?.contains("23503") == true || e.message?.contains("violates foreign key constraint") == true) {
                throw Exception("Cannot start chat: The other user hasn't set up their profile yet. Ask them to open the app.")
            }
            throw Exception("Failed to create chat room: ${e.message}")
        }
    }

    override suspend fun sendMessage(roomId: String, content: String) {
        val currentUserId = auth.currentUserOrNull()?.id ?: throw Exception("User not logged in")
        val timestamp = System.currentTimeMillis()
        val message = ChatMessage(
            roomId = roomId,
            senderId = currentUserId,
            content = content,
            timestamp = timestamp
        )
        
        try {
            postgrest["chat_messages"].insert(message)
            
            // Update last message in chat room
            val update = buildJsonObject {
                put("last_message", content)
                put("last_message_time", timestamp)
            }
            postgrest["chat_rooms"].update(update) {
                filter {
                    eq("id", roomId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun getMessages(roomId: String): Flow<List<ChatMessage>> {
        val channel = realtime.channel("room_${roomId}_${UUID.randomUUID()}")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "chat_messages"
        }.map {
            fetchMessages(roomId)
        }.onStart {
            channel.subscribe()
            emit(fetchMessages(roomId))
        }.onCompletion {
            realtime.removeChannel(channel)
        }
    }

    private suspend fun fetchMessages(roomId: String): List<ChatMessage> {
        return try {
            postgrest["chat_messages"].select {
                filter { eq("room_id", roomId) }
                order("timestamp", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }.decodeList<ChatMessage>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getChatRooms(): Flow<List<ChatRoom>> {
        val currentUserId = auth.currentUserOrNull()?.id ?: return kotlinx.coroutines.flow.emptyFlow()
        val channel = realtime.channel("chat_rooms_${UUID.randomUUID()}")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "chat_rooms"
        }.map {
            fetchChatRooms(currentUserId)
        }.onStart {
            channel.subscribe()
            emit(fetchChatRooms(currentUserId) )
        }.onCompletion {
            realtime.removeChannel(channel)
        }
    }

    private suspend fun fetchChatRooms(currentUserId: String): List<ChatRoom> {
        return try {
            postgrest["chat_rooms"].select {
                filter {
                    or {
                        eq("participant1", currentUserId)
                        eq("participant2", currentUserId)
                    }
                }
                order("last_message_time", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<ChatRoom>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getUserDetails(uid: String): ChatUser? {
        return try {
            postgrest["profiles"].select {
                filter {
                    eq("id", uid)
                }
            }.decodeSingleOrNull<ChatUser>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun ensureProfileExists(uid: String) {
        try {
            val currentUser = auth.currentUserOrNull()
            if (currentUser == null || currentUser.id != uid) {
                // We can only ensure our own profile exists
                return
            }

            val existing = postgrest["profiles"].select {
                filter { eq("id", uid) }
            }.decodeSingleOrNull<ChatUser>()
            
            if (existing == null) {
                val name = currentUser.userMetadata?.get("display_name")?.jsonPrimitive?.content
                    ?: currentUser.userMetadata?.get("full_name")?.jsonPrimitive?.content
                    ?: "No Name"
                val email = currentUser.email ?: ""
                
                val newProfile = buildJsonObject {
                    put("id", uid)
                    put("display_name", name)
                    put("email", email)
                }
                postgrest["profiles"].insert(newProfile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
