package com.live.notesapp.presentation.call

import android.app.Application
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.live.notesapp.domain.model.CallSignal
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.domain.repository.CallRepository
import com.live.notesapp.domain.repository.ChatRepository
import com.live.notesapp.notification.CallNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface IncomingCallUiState {
    data object Ringing : IncomingCallUiState
    data object Accepted : IncomingCallUiState
    data object Rejected : IncomingCallUiState
    data object Missed : IncomingCallUiState
}

@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    private val application: Application,
    private val callRepository: CallRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val notificationHelper: CallNotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<IncomingCallUiState>(IncomingCallUiState.Ringing)
    val uiState: StateFlow<IncomingCallUiState> = _uiState.asStateFlow()

    private var timeoutJob: Job? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    init {
        startRingingAndVibrating()
    }

    fun startTimeoutTimer(callId: String, roomId: String, callerId: String) {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(30_000) // 30-second missed call threshold
            if (_uiState.value is IncomingCallUiState.Ringing) {
                Log.d("IncomingCallViewModel", "Call timed out after 30s: $callId")
                handleMissed(callId, roomId, callerId)
            }
        }
    }

    private fun startRingingAndVibrating() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(application, uri)
            ringtone?.play()

            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = application.getSystemService(Application.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                application.getSystemService(Application.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 1000, 1000, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("IncomingCallViewModel", "Error starting ringtone or vibration", e)
        }
    }

    private fun stopRingingAndVibrating() {
        try {
            ringtone?.stop()
            ringtone = null
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e("IncomingCallViewModel", "Error stopping ringtone or vibration", e)
        }
    }

    fun acceptCall(callId: String) {
        stopRingingAndVibrating()
        timeoutJob?.cancel()
        notificationHelper.dismissNotification()
        _uiState.value = IncomingCallUiState.Accepted

        viewModelScope.launch {
            try {
                callRepository.updateCallStatus(callId, "accepted")
            } catch (e: Exception) {
                Log.e("IncomingCallViewModel", "Error updating call status to accepted", e)
            }
        }
    }

    fun rejectCall(callId: String, roomId: String, callerId: String) {
        stopRingingAndVibrating()
        timeoutJob?.cancel()
        notificationHelper.dismissNotification()
        _uiState.value = IncomingCallUiState.Rejected

        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId() ?: ""
                callRepository.updateCallStatus(callId, "rejected")
                chatRepository.sendCallSignal(
                    roomId,
                    CallSignal("hangup", currentUserId, callerId)
                )
            } catch (e: Exception) {
                Log.e("IncomingCallViewModel", "Error updating call status to rejected", e)
            }
        }
    }

    private fun handleMissed(callId: String, roomId: String, callerId: String) {
        stopRingingAndVibrating()
        notificationHelper.dismissNotification()
        _uiState.value = IncomingCallUiState.Missed

        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId() ?: ""
                callRepository.updateCallStatus(callId, "missed")
                chatRepository.sendCallSignal(
                    roomId,
                    CallSignal("hangup", currentUserId, callerId)
                )
            } catch (e: Exception) {
                Log.e("IncomingCallViewModel", "Error updating call status to missed", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRingingAndVibrating()
        timeoutJob?.cancel()
    }
}
