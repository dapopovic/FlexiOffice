package com.example.flexioffice.geofencing.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationPermissionManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            val REQUIRED_LOCATION_PERMISSIONS =
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )

            val BACKGROUND_LOCATION_PERMISSION = Manifest.permission.ACCESS_BACKGROUND_LOCATION
        }

        /**
         * Prüft ob die Basis Location-Berechtigungen vorhanden sind
         */
        fun hasBasicLocationPermissions(): Boolean =
            REQUIRED_LOCATION_PERMISSIONS.all { permission ->
                ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }

        /**
         * Prüft ob die Background Location-Berechtigung vorhanden ist
         */
        fun hasBackgroundLocationPermission(): Boolean =
            ActivityCompat.checkSelfPermission(
                context,
                BACKGROUND_LOCATION_PERMISSION,
            ) == PackageManager.PERMISSION_GRANTED

        /**
         * Prüft ob alle erforderlichen Berechtigungen für Geofencing vorhanden sind
         */
        fun hasAllRequiredPermissions(): Boolean = hasBasicLocationPermissions() && hasBackgroundLocationPermission()

        /**
         * Gibt die Liste der fehlenden Berechtigungen zurück
         */
        fun getMissingPermissions(): List<String> {
            val missingPermissions = mutableListOf<String>()

            // Prüfe Basis-Berechtigungen
            REQUIRED_LOCATION_PERMISSIONS.forEach { permission ->
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission)
                }
            }

            // Prüfe Background-Berechtigung (nur für Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission()) {
                missingPermissions.add(BACKGROUND_LOCATION_PERMISSION)
            }

            return missingPermissions
        }
    }
