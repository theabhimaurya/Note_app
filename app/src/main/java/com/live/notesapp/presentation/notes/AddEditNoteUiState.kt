package com.live.notesapp.presentation.notes

data class AddEditNoteUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isEditing: Boolean = false,
    val isRecognizing: Boolean = false,
    val showOcrOptions: Boolean = false,
    val showOcrConflictDialog: Boolean = false,
    val pendingRecognizedText: String = ""
)
