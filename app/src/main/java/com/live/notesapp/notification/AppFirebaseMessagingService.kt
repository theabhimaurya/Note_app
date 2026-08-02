package com.live.notesapp.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.live.notesapp.domain.manager.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var notificationHelper: CallNotificationHelper

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "onNewToken: $token")
        tokenManager.handleNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_NOTIFICATION", "onMessageReceived triggered! From=${remoteMessage.from}, Notification=${remoteMessage.notification?.title}, Data=${remoteMessage.data}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val type = data["type"] ?: data["call_type"] ?: data["event"] ?: ""
            val isCall = type.equals("INCOMING_CALL", ignoreCase = true) || 
                         type.equals("CALL", ignoreCase = true) || 
                         type.equals("video", ignoreCase = true) ||
                         data.containsKey("room_id") || 
                         data.containsKey("roomId") ||
                         data.containsKey("call_id")

            if (isCall) {
                val callId = data["call_id"] ?: data["callId"] ?: data["id"] ?: "call_${System.currentTimeMillis()}"
                val roomId = data["room_id"] ?: data["roomId"] ?: "room_${System.currentTimeMillis()}"
                val callerId = data["caller_id"] ?: data["callerId"] ?: ""
                val callerName = data["caller_name"] ?: data["callerName"] ?: remoteMessage.notification?.title ?: "Incoming Video Call"
                val callerAvatar = data["caller_avatar"] ?: data["callerAvatar"]
                val callType = data["call_type"] ?: data["callType"] ?: "video"

                Log.d("FCM_NOTIFICATION", "Displaying incoming call notification for caller=$callerName, room=$roomId, callId=$callId")
                notificationHelper.showIncomingCallNotification(
                    callId = callId,
                    roomId = roomId,
                    callerId = callerId,
                    callerName = callerName,
                    callerAvatar = callerAvatar,
                    callType = callType
                )
            } else {
                Log.d("FCM_NOTIFICATION", "Received non-call data message: $data")
            }
        } else if (remoteMessage.notification != null) {
            Log.d("FCM_NOTIFICATION", "Received notification-only message: title=${remoteMessage.notification?.title}, body=${remoteMessage.notification?.body}")
        }
    }
}
