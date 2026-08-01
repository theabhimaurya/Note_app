package com.live.notesapp.presentation.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.live.notesapp.domain.model.Note
import com.live.notesapp.domain.repository.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ViewNoteUiEvent {
    data class CopyTextClicked(val note: Note) : ViewNoteUiEvent
    object DeleteClicked : ViewNoteUiEvent
    object ConfirmDeleteClicked : ViewNoteUiEvent
    object DismissDeleteDialog : ViewNoteUiEvent
}

sealed interface ViewNoteUiEffect {
    data class CopyToClipboard(val text: String) : ViewNoteUiEffect
}

@HiltViewModel
class ViewNoteViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewNoteUiState())
    val uiState: StateFlow<ViewNoteUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ViewNoteUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private val noteId: String? = savedStateHandle.get<String>("noteId")

    init {
        loadNote()
    }

    private fun loadNote() {
        noteId?.let { id ->
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                notesRepository.getNotes().onSuccess { notes ->
                    val note = notes.find { it.id == id }
                    _uiState.update { it.copy(isLoading = false, note = note) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            }
        }
    }

    fun onEvent(event: ViewNoteUiEvent) {
        when (event) {
            is ViewNoteUiEvent.CopyTextClicked -> {
                copyNoteToClipboard(event.note)
            }
            ViewNoteUiEvent.DeleteClicked -> {
                _uiState.update { it.copy(showDeleteDialog = true) }
            }
            ViewNoteUiEvent.ConfirmDeleteClicked -> {
                _uiState.update { it.copy(showDeleteDialog = false) }
                deleteNote()
            }
            ViewNoteUiEvent.DismissDeleteDialog -> {
                _uiState.update { it.copy(showDeleteDialog = false) }
            }
        }
    }

    private fun deleteNote() {
        noteId?.let { id ->
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                notesRepository.deleteNote(id).onSuccess {
                    _uiState.update { it.copy(isLoading = false, isDeleted = true) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            }
        }
    }

    private fun copyNoteToClipboard(note: Note) {
        val formattedText = "${note.title}\n\n${note.description}"
        viewModelScope.launch {
            _uiEffect.emit(ViewNoteUiEffect.CopyToClipboard(formattedText))
        }
    }
}
