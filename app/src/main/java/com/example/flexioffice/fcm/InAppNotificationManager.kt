package com.example.flexioffice.fcm

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Manager for handling in-app notifications when the app is in foreground
 */
object InAppNotificationManager {
    const val ACTION_IN_APP_NOTIFICATION = "com.example.flexioffice.IN_APP_NOTIFICATION"
    const val EXTRA_TITLE = "title"
    const val EXTRA_BODY = "body"
    const val EXTRA_TYPE = "type"
    const val EXTRA_BOOKING_ID = "booking_id"
    const val EXTRA_USER_NAME = "user_name"
    const val EXTRA_DATE = "date"

    /**
     * Send an in-app notification broadcast
     */
    fun sendInAppNotification(
        context: Context,
        title: String,
        body: String,
        type: String? = null,
        bookingId: String? = null,
        userName: String? = null,
        date: String? = null,
    ) {
        val intent =
            Intent(ACTION_IN_APP_NOTIFICATION).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_BOOKING_ID, bookingId)
                putExtra(EXTRA_USER_NAME, userName)
                putExtra(EXTRA_DATE, date)
            }

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}
