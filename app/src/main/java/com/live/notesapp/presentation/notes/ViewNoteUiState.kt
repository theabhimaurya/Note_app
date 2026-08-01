package com.live.notesapp.presentation.notes

import com.live.notesapp.domain.model.Note

data class ViewNoteUiState(
    val isLoading: Boolean = false,
    val note: Note? = null,
    val error: String? = null,
    val isDeleted: Boolean = false,
    val showDeleteDialog: Boolean = false
)
