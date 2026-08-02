package com.live.notesapp.presentation.call

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.live.notesapp.MainActivity
import com.live.notesapp.notification.CallNotificationHelper
import com.live.notesapp.ui.theme.NotesAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IncomingCallActivity : ComponentActivity() {

    private val viewModel: IncomingCallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguard()

        val callId = intent.getStringExtra(CallNotificationHelper.EXTRA_CALL_ID) ?: ""
        val roomId = intent.getStringExtra(CallNotificationHelper.EXTRA_ROOM_ID) ?: ""
        val callerId = intent.getStringExtra(CallNotificationHelper.EXTRA_CALLER_ID) ?: ""
        val callerName = intent.getStringExtra(CallNotificationHelper.EXTRA_CALLER_NAME) ?: "Incoming Call"
        val isAutoAccept = intent.getBooleanExtra("AUTO_ACCEPT", false)

        if (isAutoAccept) {
            viewModel.acceptCall(callId)
            navigateToVideoCall(callerId, roomId)
            return
        }

        viewModel.startTimeoutTimer(callId, roomId, callerId)

        setContent {
            NotesAppTheme {
                val state by viewModel.uiState.collectAsState()

                LaunchedEffect(state) {
                    when (state) {
                        is IncomingCallUiState.Accepted -> {
                            navigateToVideoCall(callerId, roomId)
                        }
                        is IncomingCallUiState.Rejected,
                        is IncomingCallUiState.Missed -> {
                            finish()
                        }
                        IncomingCallUiState.Ringing -> {}
                    }
                }

                IncomingCallScreen(
                    callerName = callerName,
                    onAccept = {
                        viewModel.acceptCall(callId)
                    },
                    onReject = {
                        viewModel.rejectCall(callId, roomId, callerId)
                    }
                )
            }
        }
    }

    private fun navigateToVideoCall(callerId: String, roomId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO_CALL", true)
            putExtra("OTHER_USER_ID", callerId)
            putExtra("ROOM_ID", roomId)
            putExtra("IS_CALLER", false)
        }
        startActivity(intent)
        finish()
    }

    private fun turnScreenOnAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}
