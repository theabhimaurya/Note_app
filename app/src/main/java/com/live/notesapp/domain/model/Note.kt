package com.live.notesapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String? = null,
    val title: String,
    val description: String,
    val userId: String? = null,
    val createdAt: String? = null
)