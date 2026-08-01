package com.live.notesapp.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.live.notesapp.domain.model.ChatRoom
import com.live.notesapp.domain.model.ChatUser
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _chatRooms = MutableStateFlow<List<ChatRoomUiState>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoomUiState>> = _chatRooms.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val currentUserId = authRepository.getCurrentUserId()

    init {
        observeChatRooms()
        syncProfile()
    }

    private fun syncProfile() {
        viewModelScope.launch {
            authRepository.syncProfile()
        }
    }

    private fun observeChatRooms() {
        chatRepository.getChatRooms()
            .combine(_searchQuery) { rooms, query ->
                val filteredRooms = if (query.isBlank()) {
                    rooms
                } else {
                    // This is basic filtering, ideally we'd filter by user name which requires mapping first
                    rooms
                }
                filteredRooms
            }
            .onEach { rooms ->
                val uiStates = rooms.map { room ->
                    val otherUserId = if (room.participant1 == currentUserId) room.participant2 else room.participant1
                    val otherUser = chatRepository.getUserDetails(otherUserId)
                    ChatRoomUiState(
                        room = room,
                        otherUser = otherUser
                    )
                }
                
                // Final filter by name if search query exists
                val query = _searchQuery.value
                val finalUiStates = if (query.isBlank()) {
                    uiStates
                } else {
                    uiStates.filter { 
                        it.otherUser?.name?.contains(query, ignoreCase = true) == true 
                    }
                }
                
                _chatRooms.value = finalUiStates
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}

data class ChatRoomUiState(
    val room: ChatRoom,
    val otherUser: ChatUser?
)
