package com.live.notesapp.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.live.notesapp.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ViewNoteUiEvent {
    data class CopyTextClicked(val note: Note) : ViewNoteUiEvent
}

sealed interface ViewNoteUiEffect {
    data class CopyToClipboard(val text: String) : ViewNoteUiEffect
}

@HiltViewModel
class ViewNoteViewModel @Inject constructor() : ViewModel() {

    private val _uiEffect = MutableSharedFlow<ViewNoteUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: ViewNoteUiEvent) {
        when (event) {
            is ViewNoteUiEvent.CopyTextClicked -> {
                copyNoteToClipboard(event.note)
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
