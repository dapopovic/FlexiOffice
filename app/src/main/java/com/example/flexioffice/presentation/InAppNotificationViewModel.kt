package com.example.flexioffice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexioffice.fcm.InAppNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InAppNotificationState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val type: String? = null,
    val bookingId: String? = null,
    val userName: String? = null,
    val date: String? = null,
    val notificationId: String = "",
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val type: String?,
    val bookingId: String?,
    val userName: String?,
    val date: String?,
    val timestamp: Long = System.currentTimeMillis(),
)

@HiltViewModel
class InAppNotificationViewModel
    @Inject
    constructor() : ViewModel() {
        private val _notificationState = MutableStateFlow(InAppNotificationState())
        val notificationState: StateFlow<InAppNotificationState> = _notificationState.asStateFlow()

        private val notificationQueue = mutableListOf<NotificationItem>()
        private var isShowingNotification = false

        init {
            observeNotifications()
        }

        /** Initializes the notification manager and observes notifications */
        private fun observeNotifications() {
            viewModelScope.launch {
                InAppNotificationManager.notificationFlow.collect { notification ->
                    showNotification(
                        title = notification.title,
                        message = notification.body,
                        type = notification.type,
                        bookingId = notification.bookingId,
                        userName = notification.userName,
                        date = notification.date,
                    )
                }
            }
        }

        /** Shows a notification with the given details */
        private fun showNotification(
            title: String,
            message: String,
            type: String?,
            bookingId: String?,
            userName: String?,
            date: String?,
        ) {
            val notificationItem =
                NotificationItem(
                    id = System.currentTimeMillis().toString(),
                    title = title,
                    message = message,
                    type = type,
                    bookingId = bookingId,
                    userName = userName,
                    date = date,
                )

            if (isShowingNotification) {
                // Add to queue if already showing a notification
                notificationQueue.add(notificationItem)
            } else {
                // Show immediately
                displayNotification(notificationItem)
            }
        }

        /** Displays the notification on the screen */
        private fun displayNotification(item: NotificationItem) {
            isShowingNotification = true
            viewModelScope.launch {
                _notificationState.value =
                    InAppNotificationState(
                        isVisible = true,
                        title = item.title,
                        message = item.message,
                        type = item.type,
                        bookingId = item.bookingId,
                        userName = item.userName,
                        date = item.date,
                        notificationId = item.id,
                    )
            }
        }

        /** Dismisses the current notification */
        fun dismissNotification() {
            _notificationState.value = _notificationState.value.copy(isVisible = false)
            isShowingNotification = false

            // Show next notification in queue if available
            if (notificationQueue.isNotEmpty()) {
                val nextNotification = notificationQueue.removeAt(0)
                // Small delay for smooth transition
                viewModelScope.launch {
                    kotlinx.coroutines.delay(500)
                    displayNotification(nextNotification)
                }
            }
        }
    }
