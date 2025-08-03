package com.example.flexioffice.geofencing.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.flexioffice.MainActivity
import com.example.flexioffice.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeOfficeNotificationManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private const val TAG = "HomeOfficeNotificationManager"
            private const val CHANNEL_ID = "home_office_reminders"
            private const val CHANNEL_NAME = "Home Office Erinnerungen"
            private const val CHANNEL_DESCRIPTION =
                @Suppress("ktlint:standard:max-line-length")
                "Benachrichtigungen wenn Sie bei geplantem Home Office das Zuhause verlassen"
            private const val NOTIFICATION_ID = 1001
        }

        private val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        init {
            createNotificationChannel()
        }

        /**
         * Creates the notification channel for Home Office reminders
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
                    setShowBadge(true)
                }

            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification Channel created: $CHANNEL_ID")
        }

        /**
         * Shows a Home Office reminder notification
         */
        fun showHomeOfficeReminderNotification(userName: String) {
            try {
                val intent =
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("navigate_to", "calendar")
                    }

                val pendingIntent =
                    PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )

                val notification =
                    NotificationCompat
                        .Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.home_work_24px)
                        .setContentTitle("🏠 Home Office Erinnerung")
                        .setContentText(
                            "Hallo $userName! Sie haben heute Home Office geplant, aber verlassen gerade das Zuhause.",
                        ).setStyle(
                            NotificationCompat
                                .BigTextStyle()
                                .bigText(
                                    "Hallo $userName! Sie haben heute Home Office geplant, aber verlassen gerade das Zuhause. Vergessen Sie nicht, von zu Hause zu arbeiten!",
                                ),
                        ).setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setVibrate(longArrayOf(0, 300, 100, 300))
                        .build()

                notificationManager.notify(NOTIFICATION_ID, notification)
                Log.d(TAG, "Home Office Reminder-Notification sent for User: $userName")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending Home Office Reminder-Notification", e)
            }
        }

        /**
         * Removes all Home Office notifications
         */
        fun cancelAllNotifications() {
            notificationManager.cancel(NOTIFICATION_ID)
            Log.d(TAG, "All Home Office notifications removed")
        }
    }
