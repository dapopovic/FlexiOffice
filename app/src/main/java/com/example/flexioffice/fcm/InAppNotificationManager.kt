package com.example.flexioffice.fcm

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Manager for handling in-app notifications when the app is in foreground
 */
object InAppNotificationManager {
    data class InAppNotification(
        val title: String,
        val body: String,
        val type: String? = null,
        val bookingId: String? = null,
        val userName: String? = null,
        val date: String? = null,
    )

    private val _notificationFlow = MutableSharedFlow<InAppNotification>()
    val notificationFlow: SharedFlow<InAppNotification> = _notificationFlow.asSharedFlow()

    /**
     * Send an in-app notification
     */
    suspend fun sendInAppNotification(
        title: String,
        body: String,
        type: String? = null,
        bookingId: String? = null,
        userName: String? = null,
        date: String? = null,
    ) {
        val notification =
            InAppNotification(
                title = title,
                body = body,
                type = type,
                bookingId = bookingId,
                userName = userName,
                date = date,
            )
        _notificationFlow.emit(notification)
    }
}
