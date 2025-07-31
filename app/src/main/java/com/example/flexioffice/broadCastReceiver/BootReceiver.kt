package com.example.flexioffice.broadCastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.flexioffice.geofencing.GeofencingService

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Log.d(TAG, "Boot completed - checking geofencing status")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                Log.d(TAG, "Device boot or app update detected - re-registering geofences")

                // Starte Service um Geofences zu re-registrieren
                val serviceIntent =
                    Intent(context, GeofencingService::class.java).apply {
                        action = GeofencingService.ACTION_REREGISTER_GEOFENCES
                    }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
