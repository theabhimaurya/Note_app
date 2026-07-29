package com.live.notesapp.presentation.notes

import com.live.notesapp.domain.model.Note

data class NotesUiState(
    val isLoading: Boolean = false,
    val notes: List<Note> = emptyList(),
    val error: String? = null,
    val searchQuery: String = ""
)