package com.live.notesapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CallSession(
    val id: String,
    val roomId: String,
    val callerId: String,
    val receiverId: String,
    val callType: String = "video",
    val status: String = "ringing"
)
