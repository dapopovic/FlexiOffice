package com.example.flexioffice.geofencing

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.edit
import com.example.flexioffice.broadCastReceiver.GeofencingBroadcastReceiver
import com.example.flexioffice.data.model.User
import com.example.flexioffice.geofencing.permissions.LocationPermissionManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofencingManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val locationPermissionManager: LocationPermissionManager,
    ) {
        companion object {
            private const val TAG = "GeofencingManager"
            private const val HOME_GEOFENCE_ID = "home_geofence"
            private const val GEOFENCE_RADIUS_METERS = 200f // 200 Meter Radius um das Zuhause
            const val GEOFENCE_PREFS = "geofence_prefs"
            const val KEY_GEOFENCE_ACTIVE = "geofence_active"
            private const val KEY_LAST_HOME_LOCATION_LAT = "last_home_lat"
            private const val KEY_LAST_HOME_LOCATION_LNG = "last_home_lng"
        }

        private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
        private val sharedPrefs: SharedPreferences = context.getSharedPreferences(GEOFENCE_PREFS, Context.MODE_PRIVATE)

        private val geofencePendingIntent: PendingIntent by lazy {
            val intent = Intent(context, GeofencingBroadcastReceiver::class.java)

            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
        }

        /**
         * Configures geofencing for the user's home location
         */
        @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        fun setupHomeGeofence(user: User): Result<Unit> {
            return try {
                if (!user.hasHomeLocation) {
                    Log.w(TAG, "User has no home location configured")
                    return Result.failure(IllegalStateException("No home location configured"))
                }

                if (!locationPermissionManager.hasAllRequiredPermissions()) {
                    Log.w(TAG, "No location permissions granted")
                    return Result.failure(SecurityException("No location permissions granted"))
                }

                // Remove previous geofences if any
                removeGeofences()

                val geofence =
                    Geofence
                        .Builder()
                        .setRequestId(HOME_GEOFENCE_ID)
                        .setCircularRegion(
                            user.homeLatitude,
                            user.homeLongitude,
                            GEOFENCE_RADIUS_METERS,
                        ).setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                        .setNotificationResponsiveness(0) // Sofortige Benachrichtigung
                        .build()

                val geofencingRequest =
                    GeofencingRequest
                        .Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                        .addGeofence(geofence)
                        .build()

                Log.d(
                    TAG,
                    "Activating home geofence for coordinates: ${user.homeLatitude}, ${user.homeLongitude}",
                )
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                // Save status in SharedPreferences
                sharedPrefs.edit {
                    putString(KEY_LAST_HOME_LOCATION_LAT, user.homeLatitude.toString())
                        .putString(KEY_LAST_HOME_LOCATION_LNG, user.homeLongitude.toString())
                        .putBoolean(KEY_GEOFENCE_ACTIVE, true)
                }

                Log.d(
                    TAG,
                    "Home Geofence successfully activated for coordinates: ${user.homeLatitude}, ${user.homeLongitude}",
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error activating home geofence", e)
                Result.failure(e)
            }
        }

        /**
         * Removes all active geofences
         */
        fun removeGeofences(): Result<Unit> =
            try {
                geofencingClient.removeGeofences(geofencePendingIntent)

                // Remove status from SharedPreferences
                sharedPrefs
                    .edit {
                        putBoolean(KEY_GEOFENCE_ACTIVE, false)
                            .remove(KEY_LAST_HOME_LOCATION_LAT)
                            .remove(KEY_LAST_HOME_LOCATION_LNG)
                    }

                Log.d(TAG, "All geofences successfully removed")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing geofences", e)
                Result.failure(e)
            }

        /**
         * Checks if geofencing is active
         */
        fun isGeofenceActive(): Boolean = sharedPrefs.getBoolean(KEY_GEOFENCE_ACTIVE, false)

        /**
         * Retrieves the last saved home location
         */
        fun getLastHomeLocation(): Pair<Double, Double>? {
            val lat = sharedPrefs.getString(KEY_LAST_HOME_LOCATION_LAT, null)?.toDoubleOrNull()
            val lng = sharedPrefs.getString(KEY_LAST_HOME_LOCATION_LNG, null)?.toDoubleOrNull()

            return if (lat != null && lng != null) {
                Pair(lat, lng)
            } else {
                null
            }
        }
    }
