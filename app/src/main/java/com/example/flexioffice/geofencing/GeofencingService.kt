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
        Log.d(TAG, "GeofencingService erstellt")
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
     * Prüft ob eine Netzwerkverbindung verfügbar ist
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Prüft ob heute Home Office geplant ist und sendet eine Notification falls ja
     */
    suspend fun checkAndSendHomeOfficeNotification() {
        try {
            Log.d(TAG, "Prüfe Home Office Status für heute...")

            // Prüfe ob heute bereits eine Notification gesendet wurde
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val lastNotificationDate = sharedPrefs.getString(KEY_LAST_NOTIFICATION_DATE, "")

            if (lastNotificationDate == today) {
                Log.d(TAG, "Heute bereits eine Home Office Notification gesendet - überspringe")
                return
            }

            // Prüfe Netzwerkverbindung
            if (!isNetworkAvailable()) {
                Log.w(TAG, "Keine Netzwerkverbindung verfügbar - verschiebe Prüfung")
                scheduleRetry()
                return
            }
            Log.d(TAG, "Netzwerkverbindung verfügbar - fahre fort mit Home Office Prüfung")

            // Mit Timeout für Netzwerk-Operationen
            withTimeout(NETWORK_TIMEOUT_MS) {
                // Hole aktuellen User
                val currentUser = authRepository.currentUser.first()
                if (currentUser?.uid == null) {
                    Log.w(TAG, "Kein angemeldeter User gefunden")
                    return@withTimeout
                }

                val user = userRepository.getUserStream(currentUser.uid).first().getOrNull()
                if (user == null) {
                    Log.w(TAG, "User-Daten konnten nicht geladen werden")
                    return@withTimeout
                }

                // Prüfe ob User heute Home Office hat
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
                    Log.d(TAG, "User hat heute Home Office geplant - sende Notification")

                    // Sende lokale Notification
                    notificationManager.showHomeOfficeReminderNotification(user.name)

                    // Speichere dass heute bereits eine Notification gesendet wurde
                    sharedPrefs
                        .edit {
                            putString(KEY_LAST_NOTIFICATION_DATE, today)
                                .putInt(KEY_RETRY_COUNT, 0) // Reset retry count on success
                        }

                    Log.d(TAG, "Home Office Notification erfolgreich gesendet")
                } else {
                    Log.d(TAG, "Kein Home Office für heute geplant - keine Notification")
                    // Reset retry count even if no notification needed
                    sharedPrefs.edit { putInt(KEY_RETRY_COUNT, 0) }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Timeout beim Prüfen des Home Office Status - versuche später erneut ${e.message}")
            scheduleRetry()
        } catch (e: UnknownHostException) {
            Log.w(TAG, "DNS Resolution Fehler - keine Internetverbindung: ${e.message}")
            scheduleRetry()
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "Socket Timeout - schwache Verbindung: ${e.message}")
            scheduleRetry()
        } catch (e: IOException) {
            Log.w(TAG, "Netzwerk I/O Fehler: ${e.message}")
            scheduleRetry()
        } catch (e: Exception) {
            Log.e(TAG, "Unerwarteter Fehler beim Prüfen des Home Office Status", e)
            // Don't schedule retry for unexpected errors to avoid infinite loops
        }
    }

    /**
     * Plant einen erneuten Versuch für später ein (nur bei Netzwerkproblemen)
     */
    private suspend fun scheduleRetry() {
        val currentRetryCount = sharedPrefs.getInt(KEY_RETRY_COUNT, 0)

        if (currentRetryCount < MAX_RETRY_COUNT) {
            val newRetryCount = currentRetryCount + 1
            sharedPrefs.edit { putInt(KEY_RETRY_COUNT, newRetryCount) }

            Log.d(TAG, "Plane Wiederholung $newRetryCount/$MAX_RETRY_COUNT für später ein")

            val delayMs = 30000L * (1 shl (newRetryCount - 1))

            Log.d(TAG, "Plane Wiederholung $newRetryCount/$MAX_RETRY_COUNT in ${delayMs / 1000}s")

            scope.launch {
                delay(delayMs)
                checkAndSendHomeOfficeNotification()
            }
        } else {
            Log.w(TAG, "Maximale Anzahl von Wiederholungen erreicht - gebe auf")
            sharedPrefs.edit { putInt(KEY_RETRY_COUNT, 0) }
        }
    }

    /**
     * Re-registriert Geofences nach einem Geräte-Neustart oder App-Update
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private suspend fun reregisterGeofencesAfterBoot() {
        try {
            Log.d(TAG, "Re-registriere Geofences nach Boot/Update...")

            // Prüfe ob Geofencing vorher aktiv war
            if (!geofencingManager.isGeofenceActive()) {
                Log.d(TAG, "Geofencing war nicht aktiv - keine Re-Registration nötig")
                return
            }

            // Prüfe Netzwerkverbindung
            if (!isNetworkAvailable()) {
                Log.w(TAG, "Keine Netzwerkverbindung für Geofence Re-Registration")
                return
            }

            // Mit Timeout für Netzwerk-Operationen
            withTimeout(NETWORK_TIMEOUT_MS) {
                // Hole aktuellen User
                val currentUser = authRepository.currentUser.first()
                if (currentUser?.uid == null) {
                    Log.w(TAG, "Kein angemeldeter User - deaktiviere Geofencing Status")
                    geofencingManager.removeGeofences()
                    return@withTimeout
                }

                val user = userRepository.getUserStream(currentUser.uid).first().getOrNull()
                if (user == null) {
                    Log.w(TAG, "User-Daten nicht verfügbar für Geofence Re-Registration")
                    return@withTimeout
                }

                // Prüfe ob User noch Home-Koordinaten hat
                if (user.hasHomeLocation) {
                    Log.d(TAG, "Re-registriere Geofence für User: ${user.name}")

                    geofencingManager.setupHomeGeofence(user).fold(
                        onSuccess = {
                            Log.d(TAG, "Geofence erfolgreich re-registriert nach Boot/Update")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Fehler beim Re-Registrieren der Geofence", error)
                            // Setze Status auf inaktiv bei Fehlern
                            geofencingManager.removeGeofences()
                        },
                    )
                } else {
                    Log.d(TAG, "User hat keine Home-Location mehr - deaktiviere Geofencing")
                    geofencingManager.removeGeofences()
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Timeout beim Re-Registrieren der Geofences")
        } catch (e: UnknownHostException) {
            Log.w(TAG, "DNS Fehler beim Re-Registrieren der Geofences: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unerwarteter Fehler beim Re-Registrieren der Geofences", e)
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Log.d(TAG, "GeofencingService gestartet mit Action: ${intent?.action}")

        // Starte als Foreground Service
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
                // Standard-Aktion: Home Office Status prüfen
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
        Log.d(TAG, "GeofencingService beendet")
    }
}
