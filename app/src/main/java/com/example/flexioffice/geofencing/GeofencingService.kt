package com.example.flexioffice.geofencing

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.BookingRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.geofencing.GeofencingManager
import com.example.flexioffice.geofencing.notifications.HomeOfficeNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class GeofencingService : Service() {
    companion object {
        private const val TAG = "GeofencingService"
        private const val NOTIFICATION_PREFS = "home_office_notification_prefs"
        private const val KEY_LAST_NOTIFICATION_DATE = "last_notification_date"
        private const val KEY_RETRY_COUNT = "retry_count"
        private const val FOREGROUND_NOTIFICATION_ID = 2001
        private const val FOREGROUND_CHANNEL_ID = "geofencing_service_channel"
        private const val NETWORK_TIMEOUT_MS = 10000L // 10 seconds
        private const val MAX_RETRY_COUNT = 3

        // Action constants
        const val ACTION_REREGISTER_GEOFENCES = "com.example.flexioffice.action.REREGISTER_GEOFENCES"
    }

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var bookingRepository: BookingRepository

    @Inject
    lateinit var notificationManager: HomeOfficeNotificationManager

    @Inject
    lateinit var geofencingManager: GeofencingManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPrefs = getSharedPreferences(NOTIFICATION_PREFS, MODE_PRIVATE)
        createNotificationChannel()
        Log.d(TAG, "GeofencingService created")
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Geofencing Service",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Service für Home Office Erinnerungen"
                setShowBadge(false)
            }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createForegroundNotification(): Notification =
        NotificationCompat
            .Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Home Office Prüfung")
            .setContentText("Prüfe Home Office Status...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Checks if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Checks if home office is planned for today and sends a notification if so
     */
    suspend fun checkAndSendHomeOfficeNotification() {
        try {
            Log.d(TAG, "Checking home office status for today...")

            // Prüfe ob heute bereits eine Notification gesendet wurde
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val lastNotificationDate = sharedPrefs.getString(KEY_LAST_NOTIFICATION_DATE, "")

            if (lastNotificationDate == today) {
                Log.d(TAG, "Home office notification already sent today - skipping")
                return
            }

            // Prüfe Netzwerkverbindung
            if (!isNetworkAvailable()) {
                Log.w(TAG, "No network connection available - postponing check")
                scheduleRetry()
                return
            }
            Log.d(TAG, "Network connection available - continuing with home office check")

            // Mit Timeout für Netzwerk-Operationen
            withTimeout(NETWORK_TIMEOUT_MS) {
                // Hole aktuellen User
                val currentUser = authRepository.currentUser.first()
                if (currentUser?.uid == null) {
                    Log.w(TAG, "No logged in user found")
                    return@withTimeout
                }

                val user = userRepository.getUserStream(currentUser.uid).first().getOrNull()
                if (user == null) {
                    Log.w(TAG, "User data could not be loaded")
                    return@withTimeout
                }

                // Check if user has home office today
                val todayBookings =
                    bookingRepository
                        .getUserBookingsForDate(
                            userId = currentUser.uid,
                            date = LocalDate.now(),
                        ).getOrNull() ?: emptyList()

                val hasHomeOfficeToday =
                    todayBookings.any { booking ->
                        booking.status == BookingStatus.APPROVED
                    }

                if (hasHomeOfficeToday) {
                    Log.d(TAG, "User has home office planned for today - sending notification")

                    // Send local notification
                    notificationManager.showHomeOfficeReminderNotification(user.name)

                    // Save that a notification has already been sent today
                    sharedPrefs
                        .edit {
                            putString(KEY_LAST_NOTIFICATION_DATE, today)
                                .putInt(KEY_RETRY_COUNT, 0) // Reset retry count on success
                        }

                    Log.d(TAG, "Home office notification sent successfully")
                } else {
                    Log.d(TAG, "No home office planned for today - no notification")
                    // Reset retry count even if no notification needed
                    sharedPrefs.edit { putInt(KEY_RETRY_COUNT, 0) }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Timeout while checking home office status - will retry later ${e.message}")
            scheduleRetry()
        } catch (e: UnknownHostException) {
            Log.w(TAG, "DNS resolution error - no internet connection: ${e.message}")
            scheduleRetry()
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "Socket timeout - weak connection: ${e.message}")
            scheduleRetry()
        } catch (e: IOException) {
            Log.w(TAG, "Network I/O error: ${e.message}")
            scheduleRetry()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while checking home office status", e)
            // Don't schedule retry for unexpected errors to avoid infinite loops
        }
    }

    /**
     * Schedules a retry for later (only for network issues)
     */
    private suspend fun scheduleRetry() {
        val currentRetryCount = sharedPrefs.getInt(KEY_RETRY_COUNT, 0)

        if (currentRetryCount < MAX_RETRY_COUNT) {
            val newRetryCount = currentRetryCount + 1
            sharedPrefs.edit { putInt(KEY_RETRY_COUNT, newRetryCount) }

            Log.d(TAG, "Scheduling retry $newRetryCount/$MAX_RETRY_COUNT for later")

            val delayMs = 30000L * (1 shl (newRetryCount - 1))

            Log.d(TAG, "Scheduling retry $newRetryCount/$MAX_RETRY_COUNT in ${delayMs / 1000}s")

            scope.launch {
                delay(delayMs)
                checkAndSendHomeOfficeNotification()
            }
        } else {
            Log.w(TAG, "Maximum number of retries reached - giving up")
            sharedPrefs.edit { putInt(KEY_RETRY_COUNT, 0) }
        }
    }

    /**
     * Re-registers geofences after a device reboot or app update
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private suspend fun reregisterGeofencesAfterBoot() {
        try {
            Log.d(TAG, "Re-registering geofences after boot/update...")

            // Check if geofencing was active before
            if (!geofencingManager.isGeofenceActive()) {
                Log.d(TAG, "Geofencing was not active - no re-registration needed")
                return
            }

            // Check network connection
            if (!isNetworkAvailable()) {
                Log.w(TAG, "No network connection for geofence re-registration")
                return
            }

            // With timeout for network operations
            withTimeout(NETWORK_TIMEOUT_MS) {
                // Get current user
                val currentUser = authRepository.currentUser.first()
                if (currentUser?.uid == null) {
                    Log.w(TAG, "No logged in user - disabling geofencing status")
                    geofencingManager.removeGeofences()
                    return@withTimeout
                }

                val user = userRepository.getUserStream(currentUser.uid).first().getOrNull()
                if (user == null) {
                    Log.w(TAG, "User data not available for geofence re-registration")
                    return@withTimeout
                }

                // Check if user still has home coordinates
                if (user.hasHomeLocation) {
                    Log.d(TAG, "Re-registering geofence for user: ${user.name}")

                    geofencingManager.setupHomeGeofence(user).fold(
                        onSuccess = {
                            Log.d(TAG, "Geofence successfully re-registered after boot/update")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Error re-registering geofence", error)
                            // Set status to inactive on errors
                            geofencingManager.removeGeofences()
                        },
                    )
                } else {
                    Log.d(TAG, "User has no home location anymore - disabling geofencing")
                    geofencingManager.removeGeofences()
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Timeout while re-registering geofences")
        } catch (e: UnknownHostException) {
            Log.w(TAG, "DNS error while re-registering geofences: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while re-registering geofences", e)
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Log.d(TAG, "GeofencingService started with Action: ${intent?.action}")

        // Start as Foreground Service
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification())

        when (intent?.action) {
            ACTION_REREGISTER_GEOFENCES -> {
                scope.launch {
                    try {
                        reregisterGeofencesAfterBoot()
                    } finally {
                        stopSelf()
                    }
                }
            }
            else -> {
                // Default action: Check home office status
                scope.launch {
                    try {
                        checkAndSendHomeOfficeNotification()
                    } finally {
                        stopSelf()
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "GeofencingService stopped")
    }
}
