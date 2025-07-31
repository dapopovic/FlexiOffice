package com.example.flexioffice.geofencing

import android.util.Log
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.geofencing.permissions.LocationPermissionManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofencingInitializer
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val geofencingManager: GeofencingManager,
        private val locationPermissionManager: LocationPermissionManager,
    ) {
        companion object {
            private const val TAG = "GeofencingInitializer"
        }

        /**
         * Initialisiert Geofencing beim App-Start falls alle Voraussetzungen erfüllt sind
         */
        suspend fun initializeGeofencingOnAppStart() {
            try {
                Log.d(TAG, "Checking if geofencing should be initialized...")

                // Prüfe ob User angemeldet ist
                val currentUser = authRepository.currentUser.first()
                if (currentUser?.uid == null) {
                    Log.d(TAG, "No user logged in, skipping geofencing initialization")
                    return
                }

                // Lade User-Daten
                val user = userRepository.getUser(currentUser.uid).getOrNull()
                if (user == null) {
                    Log.w(TAG, "Could not load user data")
                    return
                }

                // Prüfe ob User Home-Location konfiguriert hat
                if (!user.hasHomeLocation) {
                    Log.d(TAG, "User has no home location configured, skipping geofencing")
                    return
                }

                // Prüfe Berechtigungen
                if (!locationPermissionManager.hasAllRequiredPermissions()) {
                    Log.d(TAG, "Missing location permissions, skipping geofencing")
                    cleanupGeofencingOnLogout()
                    return
                }

                // Prüfe ob Geofencing bereits aktiv ist
                if (geofencingManager.isGeofenceActive()) {
                    Log.d(TAG, "Geofencing already active")
                    return
                }

                // Initialisiere Geofencing
                Log.d(TAG, "Initializing geofencing for user: ${user.name}")
                geofencingManager.setupHomeGeofence(user).fold(
                    onSuccess = {
                        Log.d(TAG, "Geofencing successfully initialized")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to initialize geofencing", error)
                    },
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during geofencing initialization", e)
            }
        }

        /**
         * Deaktiviert Geofencing beim Logout
         */
        fun cleanupGeofencingOnLogout() {
            try {
                Log.d(TAG, "Cleaning up geofencing on logout...")
                geofencingManager.removeGeofences().fold(
                    onSuccess = {
                        Log.d(TAG, "Geofencing successfully cleaned up")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to cleanup geofencing", error)
                    },
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during geofencing cleanup", e)
            }
        }
    }
