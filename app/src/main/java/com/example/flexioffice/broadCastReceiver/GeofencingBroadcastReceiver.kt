package com.example.flexioffice.broadCastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.example.flexioffice.geofencing.GeofencingService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofencingBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "GeofencingReceiver"
    private const val PREFS_NAME = "geofence_receiver_prefs"
    private const val KEY_LAST_EXIT_TS = "last_exit_timestamp"
    private const val EXIT_DEBOUNCE_MS = 60_000L
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Log.d(TAG, "Geofencing event received")
        Log.d(TAG, "Intent action: ${intent.action}")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        // Check the geofence transition type
        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            triggeringGeofences?.forEach { geofence ->
                Log.d(TAG, "Exit from geofence: ${geofence.requestId}")

                if (geofence.requestId == "home_geofence") {
                    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val now = System.currentTimeMillis()
                    val lastExit = prefs.getLong(KEY_LAST_EXIT_TS, 0L)
                    if (now - lastExit < EXIT_DEBOUNCE_MS) {
                        Log.d(TAG, "Exit event debounced (within ${EXIT_DEBOUNCE_MS/1000}s)")
                        return
                    }
                    prefs.edit().putLong(KEY_LAST_EXIT_TS, now).apply()

                    Log.d(TAG, "User left home geofence - starting GeofencingService")
                    val serviceIntent = Intent(context, GeofencingService::class.java)
                    context.startForegroundService(serviceIntent)
                }
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            triggeringGeofences?.forEach { geofence ->
                Log.d(TAG, "Enter geofence: ${geofence.requestId}")
            }
        } else {
            Log.d(TAG, "Other geofence transition: $geofenceTransition")
        }
    }
}
