package com.live.notesapp.domain.repository

import com.live.notesapp.domain.model.ChatMessage
import com.live.notesapp.domain.model.ChatRoom
import com.live.notesapp.domain.model.ChatUser
import com.live.notesapp.domain.model.CallSignal
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun createOrGetChatRoom(otherUserId: String): String
    suspend fun sendMessage(roomId: String, content: String)
    fun getMessages(roomId: String): Flow<List<ChatMessage>>
    fun getChatRooms(): Flow<List<ChatRoom>>
    suspend fun getUserDetails(uid: String): ChatUser?
    
    suspend fun sendCallSignal(roomId: String, signal: CallSignal)
    fun observeCallSignals(roomId: String): Flow<CallSignal>
}
