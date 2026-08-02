package com.live.notesapp.presentation.call

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.domain.repository.ChatRepository
import com.live.notesapp.webrtc.WebRtcSessionManager
import com.live.notesapp.domain.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoCallViewModel @Inject constructor(
    private val application: Application,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val callRepository: CallRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val otherUserId: String = checkNotNull(savedStateHandle["otherUserId"])
    private val roomId: String = checkNotNull(savedStateHandle["roomId"])
    val isCaller: Boolean = savedStateHandle.get<String>("isCaller")?.toBooleanStrictOrNull()
        ?: savedStateHandle.get<Boolean>("isCaller")
        ?: false
    
    private val _otherUser = MutableStateFlow<com.live.notesapp.domain.model.ChatUser?>(null)
    val otherUser: StateFlow<com.live.notesapp.domain.model.ChatUser?> = _otherUser

    private var _sessionManager = MutableStateFlow<WebRtcSessionManager?>(null)
    val sessionManager: StateFlow<WebRtcSessionManager?> = _sessionManager
    private var hasCreatedSession = false

    init {
        viewModelScope.launch {
            _otherUser.value = chatRepository.getUserDetails(otherUserId)
        }
        if (isCaller) {
            triggerCallSessionCreation()
        }
    }

    private fun triggerCallSessionCreation() {
        if (hasCreatedSession) return
        hasCreatedSession = true
        val currentUserId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                android.util.Log.d("VideoCallViewModel", "Initiating call session in DB: caller=$currentUserId, receiver=$otherUserId, room=$roomId")
                callRepository.createCallSession(
                    callerId = currentUserId,
                    receiverId = otherUserId,
                    roomId = roomId,
                    callType = "video"
                )
            } catch (e: Exception) {
                android.util.Log.e("VideoCallViewModel", "Error creating call session: ${e.message}", e)
            }
        }
    }

    fun initializeSession() {
        if (_sessionManager.value != null) return
        val cachedUserId = authRepository.getCurrentUserId()
        if (cachedUserId != null) {
            _sessionManager.value = WebRtcSessionManager(
                context = application,
                chatRepository = chatRepository,
                currentUserId = cachedUserId,
                otherUserId = otherUserId,
                roomId = roomId
            )
            return
        }
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                if (user != null && _sessionManager.value == null) {
                    _sessionManager.value = WebRtcSessionManager(
                        context = application,
                        chatRepository = chatRepository,
                        currentUserId = user.id,
                        otherUserId = otherUserId,
                        roomId = roomId
                    )
                }
            }
        }
    }

    fun startCall() {
        _sessionManager.value?.startCall()
        if (isCaller) {
            triggerCallSessionCreation()
        }
    }

    fun endCall() {
        _sessionManager.value?.endCall()
    }

    fun setMicEnabled(enabled: Boolean) {
        _sessionManager.value?.setMicEnabled(enabled)
    }

    fun setCameraEnabled(enabled: Boolean) {
        _sessionManager.value?.setCameraEnabled(enabled)
    }

    override fun onCleared() {
        super.onCleared()
        _sessionManager.value?.endCall()
    }
}
