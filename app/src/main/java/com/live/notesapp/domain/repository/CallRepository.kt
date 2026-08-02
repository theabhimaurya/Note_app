package com.live.notesapp.domain.repository

import com.live.notesapp.domain.model.CallSession

interface CallRepository {
    suspend fun createCallSession(callerId: String, receiverId: String, roomId: String, callType: String = "video"): CallSession
    suspend fun updateCallStatus(callId: String, status: String)
    suspend fun updateFcmToken(userId: String, token: String)
    suspend fun getCallSession(callId: String): CallSession?
}
