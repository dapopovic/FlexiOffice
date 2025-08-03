package com.example.flexioffice.fcm

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.flexioffice.MainActivity
import com.example.flexioffice.R
import com.example.flexioffice.fcm.FCMTokenManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FlexiOfficeMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "flexioffice_booking_updates"
        private const val CHANNEL_NAME = "Buchungsanfragen Updates"
        private const val CHANNEL_DESCRIPTION = "Benachrichtigungen über Buchungsanfragen-Status"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * Creates the notification channel for FCM messages
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed FCM token: $token")

        sendTokenToServer(token)
    }

    /**
     * Handles incoming FCM messages
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        } else {
            // Only show notification payload if there's no data payload
            // This prevents duplicate notifications when both payloads are present
            remoteMessage.notification?.let {
                Log.d(TAG, "Message Notification Body: ${it.body}")
                showNotification(it.title, it.body)
            }
        }
    }

    /**
     * Handles data messages with specific types
     */
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
                        "APPROVED" -> "Ihr Home-Office-Antrag${date?.let { " für $it" } ?: ""} wurde genehmigt."
                        "DECLINED" -> "Ihr Home-Office-Antrag${date?.let { " für $it" } ?: ""} wurde leider abgelehnt."
                        else -> "Der Status Ihrer Buchung wurde geändert."
                    }

                showNotificationWithType(title, body, type, bookingId, userName, date)
            }
            "new_booking_request" -> {
                val title = "Neue Buchungsanfrage 📋"
                val body = "${userName ?: "Ein Mitarbeiter"} möchte Home-Office${date?.let { " am $it" } ?: ""}"
                showNotificationWithType(title, body, type, bookingId, userName, date)
            }
            else -> {
                Log.w(TAG, "Unknown message type: $type")
            }
        }
    }

    /**
     * Shows a notification with the given title and body
     */
    private fun showNotification(
        title: String?,
        body: String?,
        bookingId: String? = null,
    ) {
        // If app is in foreground, send in-app notification instead
        if (isAppInForeground()) {
            Log.d(TAG, "App is in foreground, sending in-app notification")
            serviceScope.launch {
                InAppNotificationManager.sendInAppNotification(
                    title = title ?: "FlexiOffice",
                    body = body ?: "Sie haben eine neue Nachricht",
                    bookingId = bookingId,
                )
            }
            return
        }

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

    /**
     * Shows a notification with type-specific handling
     */
    private fun showNotificationWithType(
        title: String?,
        body: String?,
        type: String?,
        bookingId: String? = null,
        userName: String? = null,
        date: String? = null,
    ) {
        // If app is in foreground, send in-app notification instead
        if (isAppInForeground()) {
            Log.d(TAG, "App is in foreground, sending in-app notification")
            serviceScope.launch {
                InAppNotificationManager.sendInAppNotification(
                    title = title ?: "FlexiOffice",
                    body = body ?: "Sie haben eine neue Nachricht",
                    type = type,
                    bookingId = bookingId,
                    userName = userName,
                    date = date,
                )
            }
            return
        }

        // Show regular notification if app is in background
        showNotification(title, body, bookingId)
    }

    /**
     * Creates the notification channel for FCM messages
     */
    private fun createNotificationChannel() {
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

    /**
     * Sends the FCM token to the server for the current user
     */
    private fun sendTokenToServer(token: String) {
        serviceScope.launch {
            try {
                val fcmTokenManager =
                    FCMTokenManager(
                        FirebaseMessaging.getInstance(),
                        FirebaseFirestore.getInstance(),
                        FirebaseAuth.getInstance(),
                    )
                fcmTokenManager.updateToken(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send FCM token to server: ${e.message}")
            }
        }
    }

    /**
     * Check if the app is currently in the foreground
     * Uses a simplified approach suitable for modern Android versions
     */
    private fun isAppInForeground(): Boolean {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appProcesses = activityManager.runningAppProcesses ?: return false

            val packageName = packageName
            for (appProcess in appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    appProcess.processName == packageName
                ) {
                    Log.d(TAG, "App is in foreground")
                    return true
                }
            }
            Log.d(TAG, "App is in background")
            return false
        } catch (e: Exception) {
            Log.w(TAG, "Could not determine app state, showing notification anyway", e)
            // If we can't determine the state, show the notification to be safe
            return false
        }
    }
}
