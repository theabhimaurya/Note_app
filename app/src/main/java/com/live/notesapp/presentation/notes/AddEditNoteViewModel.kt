package com.live.notesapp.presentation.notes

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.live.notesapp.domain.model.Note
import com.live.notesapp.domain.repository.NotesRepository
import com.live.notesapp.domain.usecase.RecognizeTextUseCase
import com.live.notesapp.utils.getSupabaseErrorMessage
import io.github.jan.supabase.auth.Auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditNoteViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val recognizeTextUseCase: RecognizeTextUseCase,
    private val auth: Auth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditNoteUiState())
    val uiState: StateFlow<AddEditNoteUiState> = _uiState.asStateFlow()

    private var currentNoteId: String? = savedStateHandle.get<String>("noteId")?.takeIf { it != "null" }

    init {
        loadNote(currentNoteId)
    }

    fun loadNote(id: String?) {
        currentNoteId = id
        _uiState.update { it.copy(
            isEditing = id != null,
            title = "",
            description = "",
            isSuccess = false,
            error = null
        ) }
        id?.let { noteId ->
            viewModelScope.launch {
                // Fetch note details if editing
                notesRepository.getNotes().onSuccess { notes ->
                    notes.find { it.id == noteId }?.let { note ->
                        _uiState.update { it.copy(
                            title = note.title,
                            description = note.description
                        ) }
                    }
                }
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onContentChange(content: String) {
        _uiState.update { it.copy(description = content) }
    }

    fun deleteNote() {
        currentNoteId?.let { id ->
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                notesRepository.deleteNote(id).onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
            }
        }
    }

    fun saveNote() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            if (_uiState.value.title.isBlank() || _uiState.value.description.isBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "Title and description cannot be empty") }
                return@launch
            }

            val currentUserId = auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not authenticated") }
                return@launch
            }
            val note = Note(
                id = currentNoteId,
                title = _uiState.value.title,
                description = _uiState.value.description,
                userId = currentUserId
            )
            val result = if (currentNoteId == null) {
                notesRepository.addNote(note)
            } else {
                notesRepository.updateNote(note)
            }

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.getSupabaseErrorMessage()) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onScanTextClicked() {
        _uiState.update { it.copy(showOcrOptions = true) }
    }

    fun onDismissOcrOptions() {
        _uiState.update { it.copy(showOcrOptions = false) }
    }

    fun onImageSelected(uri: Uri) {
        _uiState.update { it.copy(showOcrOptions = false, isRecognizing = true) }
        viewModelScope.launch {
            recognizeTextUseCase(uri)
                .onSuccess { text ->
                    handleRecognizedText(text)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isRecognizing = false, error = e.message ?: "Failed to recognize text") }
                }
        }
    }

    private fun handleRecognizedText(text: String) {
        if (_uiState.value.description.isBlank()) {
            _uiState.update { it.copy(description = text, isRecognizing = false) }
        } else {
            _uiState.update { it.copy(pendingRecognizedText = text, showOcrConflictDialog = true, isRecognizing = false) }
        }
    }

    fun onReplaceText() {
        _uiState.update { it.copy(
            description = it.pendingRecognizedText,
            showOcrConflictDialog = false,
            pendingRecognizedText = ""
        ) }
    }

    fun onAppendText() {
        val currentText = _uiState.value.description
        val pendingText = _uiState.value.pendingRecognizedText
        val newDescription = if (currentText.endsWith("\n") || currentText.isBlank()) {
            currentText + pendingText
        } else {
            currentText + "\n" + pendingText
        }
        _uiState.update { it.copy(
            description = newDescription,
            showOcrConflictDialog = false,
            pendingRecognizedText = ""
        ) }
    }

    fun onDismissConflictDialog() {
        _uiState.update { it.copy(showOcrConflictDialog = false, pendingRecognizedText = "") }
    }
}
