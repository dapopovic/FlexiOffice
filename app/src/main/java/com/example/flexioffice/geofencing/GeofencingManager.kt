package com.example.flexioffice.geofencing

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import com.example.flexioffice.broadCastReceiver.GeofencingBroadcastReceiver
import com.example.flexioffice.data.model.User
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
         * Konfiguriert Geofencing für das Zuhause des Benutzers
         */
        @RequiresApi(Build.VERSION_CODES.S)
        fun setupHomeGeofence(user: User): Result<Unit> {
            return try {
                if (!user.hasHomeLocation) {
                    Log.w(TAG, "User hat keine Home-Location konfiguriert")
                    return Result.failure(IllegalStateException("Keine Home-Location konfiguriert"))
                }

                if (!hasLocationPermissions()) {
                    Log.w(TAG, "Keine Location-Berechtigung vorhanden")
                    return Result.failure(SecurityException("Keine Location-Berechtigung"))
                }

                // Entferne vorherige Geofences falls vorhanden
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
                    "Aktiviere Home Geofence für Koordinaten: ${user.homeLatitude}, ${user.homeLongitude}",
                )
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                // Speichere Status in SharedPreferences
                sharedPrefs.edit {
                    putString(KEY_LAST_HOME_LOCATION_LAT, user.homeLatitude.toString())
                        .putString(KEY_LAST_HOME_LOCATION_LNG, user.homeLongitude.toString())
                        .putBoolean(KEY_GEOFENCE_ACTIVE, true)
                }

                Log.d(
                    TAG,
                    "Home Geofence erfolgreich aktiviert für Koordinaten: ${user.homeLatitude}, ${user.homeLongitude}",
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Aktivieren des Home Geofence", e)
                Result.failure(e)
            }
        }

        /**
         * Entfernt alle aktiven Geofences
         */
        fun removeGeofences(): Result<Unit> =
            try {
                geofencingClient.removeGeofences(geofencePendingIntent)

                // Lösche Status in SharedPreferences
                sharedPrefs
                    .edit {
                        putBoolean(KEY_GEOFENCE_ACTIVE, false)
                            .remove(KEY_LAST_HOME_LOCATION_LAT)
                            .remove(KEY_LAST_HOME_LOCATION_LNG)
                    }

                Log.d(TAG, "Alle Geofences erfolgreich entfernt")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Entfernen der Geofences", e)
                Result.failure(e)
            }

        /**
         * Prüft ob Geofencing aktiv ist
         */
        fun isGeofenceActive(): Boolean = sharedPrefs.getBoolean(KEY_GEOFENCE_ACTIVE, false)

        /**
         * Holt die letzte gespeicherte Home-Location
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

        /**
         * Prüft ob alle erforderlichen Location-Berechtigungen vorhanden sind
         */
        private fun hasLocationPermissions(): Boolean =
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
    }
