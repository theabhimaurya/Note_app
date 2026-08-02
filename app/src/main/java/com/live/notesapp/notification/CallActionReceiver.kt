package com.live.notesapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.live.notesapp.domain.model.CallSignal
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.domain.repository.CallRepository
import com.live.notesapp.domain.repository.ChatRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CallActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var callRepository: CallRepository

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var notificationHelper: CallNotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra(CallNotificationHelper.EXTRA_CALL_ID) ?: return
        val roomId = intent.getStringExtra(CallNotificationHelper.EXTRA_ROOM_ID) ?: return
        val callerId = intent.getStringExtra(CallNotificationHelper.EXTRA_CALLER_ID) ?: return

        when (intent.action) {
            CallNotificationHelper.ACTION_REJECT_CALL -> {
                Log.d("CallActionReceiver", "Rejecting incoming call $callId")
                notificationHelper.dismissNotification()

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val currentUserId = authRepository.getCurrentUserId() ?: ""
                        callRepository.updateCallStatus(callId, "rejected")
                        chatRepository.sendCallSignal(
                            roomId,
                            CallSignal(
                                type = "hangup",
                                senderId = currentUserId,
                                receiverId = callerId
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("CallActionReceiver", "Error rejecting call", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
