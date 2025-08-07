package com.example.flexioffice.geofencing.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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

            const val BACKGROUND_LOCATION_PERMISSION = Manifest.permission.ACCESS_BACKGROUND_LOCATION
        }

        /**
         * Checks if the basic location permissions are granted
         */
        fun hasBasicLocationPermissions(): Boolean =
            REQUIRED_LOCATION_PERMISSIONS.all { permission ->
                ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }

        /**
         * Checks if the background location permission is granted
         */
        fun hasBackgroundLocationPermission(): Boolean =
            ActivityCompat.checkSelfPermission(
                context,
                BACKGROUND_LOCATION_PERMISSION,
            ) == PackageManager.PERMISSION_GRANTED

        /**
         * Checks if all required permissions for geofencing are granted
         */
        fun hasAllRequiredPermissions(): Boolean = hasBasicLocationPermissions() && hasBackgroundLocationPermission()

        /**
         * Returns the list of missing permissions
         */
        fun getMissingPermissions(): List<String> {
            val missingPermissions = mutableListOf<String>()

            // Check basic permissions
            REQUIRED_LOCATION_PERMISSIONS.forEach { permission ->
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission)
                }
            }

            // Check background location permission (only for Android 10+)
            if (!hasBackgroundLocationPermission()) {
                missingPermissions.add(BACKGROUND_LOCATION_PERMISSION)
            }

            return missingPermissions
        }
    }
