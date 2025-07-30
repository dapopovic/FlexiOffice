package com.example.flexioffice.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.flexioffice.fcm.InAppNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    constructor(
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _notificationState = MutableStateFlow(InAppNotificationState())
        val notificationState: StateFlow<InAppNotificationState> = _notificationState.asStateFlow()

        private val notificationQueue = mutableListOf<NotificationItem>()
        private var isShowingNotification = false

        private val notificationReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    intent?.let { receivedIntent ->
                        val title =
                            receivedIntent.getStringExtra(InAppNotificationManager.EXTRA_TITLE)
                                ?: ""
                        val body =
                            receivedIntent.getStringExtra(InAppNotificationManager.EXTRA_BODY)
                                ?: ""
                        val type =
                            receivedIntent.getStringExtra(InAppNotificationManager.EXTRA_TYPE)
                        val bookingId =
                            receivedIntent.getStringExtra(
                                InAppNotificationManager.EXTRA_BOOKING_ID,
                            )
                        val userName =
                            receivedIntent.getStringExtra(
                                InAppNotificationManager.EXTRA_USER_NAME,
                            )
                        val date =
                            receivedIntent.getStringExtra(InAppNotificationManager.EXTRA_DATE)

                        showNotification(title, body, type, bookingId, userName, date)
                    }
                }
            }

        init {
            registerReceiver()
        }

        private fun registerReceiver() {
            val filter = IntentFilter(InAppNotificationManager.ACTION_IN_APP_NOTIFICATION)
            LocalBroadcastManager.getInstance(context).registerReceiver(notificationReceiver, filter)
        }

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

        override fun onCleared() {
            super.onCleared()
            LocalBroadcastManager.getInstance(context).unregisterReceiver(notificationReceiver)
        }
    }
