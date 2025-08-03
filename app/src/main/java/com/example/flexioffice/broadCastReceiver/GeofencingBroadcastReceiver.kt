package com.example.flexioffice.broadCastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.flexioffice.geofencing.GeofencingService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofencingBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "GeofencingReceiver"
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

        // Prüfe den Geofence-Übergang
        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            triggeringGeofences?.forEach { geofence ->
                Log.d(TAG, "Exit from geofence: ${geofence.requestId}")

                if (geofence.requestId == "home_geofence") {
                    Log.d(TAG, "User left home geofence - starting GeofencingService")

                    // Start GeofencingService to check home office status
                    val serviceIntent = Intent(context, GeofencingService::class.java)
                    context.startForegroundService(serviceIntent)
                }
            }
        } else {
            Log.d(TAG, "Unknown geofence transition: $geofenceTransition")
        }
    }
}
