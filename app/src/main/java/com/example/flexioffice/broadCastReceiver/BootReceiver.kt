package com.example.flexioffice.broadCastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
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
            Intent.ACTION_PACKAGE_REPLACED,
            -> {
                Log.d(TAG, "Device boot or app update detected - re-registering geofences")

                // check permissions and re-register geofences {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "Notification permission not granted, cannot re-register geofences")
                    return
                }
                if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "Location permission not granted, cannot re-register geofences")
                    return
                }
                // Start GeofencingService to re-register geofences
                val serviceIntent =
                    Intent(context, GeofencingService::class.java).apply {
                        action = GeofencingService.ACTION_REREGISTER_GEOFENCES
                    }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
