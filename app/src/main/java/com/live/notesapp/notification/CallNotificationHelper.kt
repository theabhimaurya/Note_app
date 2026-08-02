package com.live.notesapp.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.live.notesapp.R
import com.live.notesapp.presentation.call.IncomingCallActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID_CALLS = "channel_incoming_video_calls"
        const val NOTIFICATION_ID = 2001
        const val ACTION_ACCEPT_CALL = "com.live.notesapp.ACTION_ACCEPT_CALL"
        const val ACTION_REJECT_CALL = "com.live.notesapp.ACTION_REJECT_CALL"

        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_ROOM_ID = "extra_room_id"
        const val EXTRA_CALLER_ID = "extra_caller_id"
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_CALLER_AVATAR = "extra_caller_avatar"
        const val EXTRA_CALL_TYPE = "extra_call_type"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID_CALLS,
                "Incoming Video Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority incoming call alerts"
                setSound(ringtoneUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 1000, 1000, 1000)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showIncomingCallNotification(
        callId: String,
        roomId: String,
        callerId: String,
        callerName: String,
        callerAvatar: String?,
        callType: String
    ) {
        val callerPerson = Person.Builder()
            .setName(callerName)
            .setImportant(true)
            .build()

        // 1. Full-Screen Intent (Launches IncomingCallActivity directly when screen is locked or asleep)
        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_ROOM_ID, roomId)
            putExtra(EXTRA_CALLER_ID, callerId)
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_CALLER_AVATAR, callerAvatar)
            putExtra(EXTRA_CALL_TYPE, callType)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Reject PendingIntent (Broadcast to CallActionReceiver)
        val rejectIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = ACTION_REJECT_CALL
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_ROOM_ID, roomId)
            putExtra(EXTRA_CALLER_ID, callerId)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            context,
            callId.hashCode() + 1,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Accept PendingIntent (Opens IncomingCallActivity directly with auto-accept flag)
        val acceptIntent = Intent(context, IncomingCallActivity::class.java).apply {
            action = ACTION_ACCEPT_CALL
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_ROOM_ID, roomId)
            putExtra(EXTRA_CALLER_ID, callerId)
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_CALLER_AVATAR, callerAvatar)
            putExtra(EXTRA_CALL_TYPE, callType)
            putExtra("AUTO_ACCEPT", true)
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode() + 2,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        // 4. Build CallStyle Notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_CALLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(callerName)
            .setContentText("Incoming Video Call")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setSound(ringtoneUri)
            .setVibrate(longArrayOf(0, 1000, 1000, 1000, 1000))
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    callerPerson,
                    rejectPendingIntent,
                    acceptPendingIntent
                )
            )

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
