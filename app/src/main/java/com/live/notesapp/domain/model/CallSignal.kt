package com.live.notesapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CallSignal(
    val type: String, // "offer", "answer", "ice_candidate", "hangup"
    val senderId: String,
    val receiverId: String,
    val data: String? = null, // SDP string or ICE candidate JSON
    val timestamp: Long = System.currentTimeMillis()
)
