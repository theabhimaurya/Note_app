package com.live.notesapp.presentation.chat

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.live.notesapp.domain.model.CallSignal
import com.live.notesapp.domain.model.ChatMessage
import com.live.notesapp.domain.model.ChatUser
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.domain.repository.CallRepository
import com.live.notesapp.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val callRepository: CallRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _otherUser = mutableStateOf<ChatUser?>(null)
    val otherUser: State<ChatUser?> = _otherUser

    private val _currentUserId = mutableStateOf<String?>(null)
    val currentUserId: State<String?> = _currentUserId

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _currentRoomId = mutableStateOf<String?>(null)
    val currentRoomId: State<String?> = _currentRoomId

    private val _incomingCall = mutableStateOf<CallSignal?>(null)
    val incomingCall: State<CallSignal?> = _incomingCall

    private var signalingJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                _currentUserId.value = user?.id
            }
        }
    }

    fun initChat(otherUserId: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                _otherUser.value = chatRepository.getUserDetails(otherUserId)
                val roomId = chatRepository.createOrGetChatRoom(otherUserId)
                _currentRoomId.value = roomId
                observeSignaling(roomId)
                chatRepository.getMessages(roomId).collectLatest {
                    _messages.value = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.message ?: "An unexpected error occurred"
            }
        }
    }

    fun sendMessage(content: String) {
        val roomId = _currentRoomId.value ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(roomId, content)
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }

    private fun observeSignaling(roomId: String) {
        signalingJob?.cancel()
        signalingJob = viewModelScope.launch {
            chatRepository.observeCallSignals(roomId).collectLatest { signal ->
                if (signal.type == "offer" && signal.senderId != _currentUserId.value) {
                    _incomingCall.value = signal
                } else if (signal.type == "hangup") {
                    _incomingCall.value = null
                }
            }
        }
    }

    fun initiateCall(otherUserId: String, roomId: String) {
        val currentUserId = _currentUserId.value ?: authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                callRepository.createCallSession(
                    callerId = currentUserId,
                    receiverId = otherUserId,
                    roomId = roomId,
                    callType = "video"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismissCall() {
        _incomingCall.value = null
    }
}
