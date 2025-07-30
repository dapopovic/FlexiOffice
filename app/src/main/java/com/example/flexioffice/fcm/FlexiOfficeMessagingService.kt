package com.example.flexioffice.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.flexioffice.MainActivity
import com.example.flexioffice.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FlexiOfficeMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "flexioffice_booking_updates"
        private const val CHANNEL_NAME = "Buchungsanfragen Updates"
        private const val CHANNEL_DESCRIPTION = "Benachrichtigungen über Buchungsanfragen-Status"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed FCM token: $token")

        // TODO: Send the token to your server/Firestore
        // This should be handled by FCMTokenManager
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(it.title, it.body)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        val bookingId = data["bookingId"]
        val status = data["status"]
        val userName = data["userName"]
        val date = data["date"]

        when (type) {
            "booking_status_update" -> {
                val title =
                    when (status) {
                        "APPROVED" -> "Home-Office genehmigt! ✅"
                        "DECLINED" -> "Home-Office abgelehnt ❌"
                        else -> "Buchungsstatus geändert"
                    }

                val body =
                    when (status) {
                        "APPROVED" -> "Ihr Home-Office-Antrag für $date wurde genehmigt."
                        "DECLINED" -> "Ihr Home-Office-Antrag für $date wurde leider abgelehnt."
                        else -> "Der Status Ihrer Buchung wurde geändert."
                    }

                showNotification(title, body, bookingId)
            }
            "new_booking_request" -> {
                val title = "Neue Buchungsanfrage 📋"
                val body = "$userName möchte Home-Office am $date"
                showNotification(title, body, bookingId)
            }
            else -> {
                Log.w(TAG, "Unknown message type: $type")
            }
        }
    }

    private fun showNotification(
        title: String?,
        body: String?,
        bookingId: String? = null,
    ) {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // Navigate to appropriate screen based on notification
                bookingId?.let {
                    putExtra("booking_id", it)
                    putExtra("navigate_to", "requests")
                }
            }

        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notificationBuilder =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.assignment_24px)
                .setContentTitle(title ?: "FlexiOffice")
                .setContentText(body ?: "Sie haben eine neue Nachricht")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    enableLights(true)
                }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendTokenToServer(token: String) {
        // This will be handled by FCMTokenManager
        Log.d(TAG, "Token should be sent to server: $token")
    }
}
