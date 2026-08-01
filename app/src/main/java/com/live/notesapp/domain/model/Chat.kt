package com.live.notesapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRoom(
    val id: String? = null,
    val participant1: String, // UID of user 1
    val participant2: String, // UID of user 2
    @SerialName("last_message")
    val lastMessage: String? = null,
    @SerialName("last_message_time")
    val lastMessageTime: Long? = null,
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ChatMessage(
    val id: String? = null,
    @SerialName("room_id")
    val roomId: String,
    @SerialName("sender_id")
    val senderId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("is_read")
    val isRead: Boolean = false
)

@Serializable
data class ChatUser(
    val id: String,
    @SerialName("display_name")
    val name: String? = null,
    val email: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)
