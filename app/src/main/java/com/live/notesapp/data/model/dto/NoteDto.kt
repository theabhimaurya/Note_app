package com.live.notesapp.data.model.dto

import com.live.notesapp.domain.model.Note
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

fun NoteDto.toDomain(): Note {
    return Note(
        id = id,
        title = title,
        description = description,
        userId = userId,
        createdAt = createdAt
    )
}

fun Note.toDto(): NoteDto {
    return NoteDto(
        id = id,
        title = title,
        description = description,
        userId = userId,
        createdAt = createdAt
    )
}