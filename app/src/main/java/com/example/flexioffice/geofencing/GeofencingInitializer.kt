package com.example.flexioffice.geofencing

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.geofencing.permissions.LocationPermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
         * Initializes geofencing on app start if all prerequisites are met
         */
        @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        suspend fun initializeGeofencingOnAppStart() {
            try {
                Log.d(TAG, "Checking if geofencing should be initialized...")

                // Check if user is logged in
                val currentUser = authRepository.currentUser.first()
                if (currentUser?.uid == null) {
                    Log.d(TAG, "No user logged in, skipping geofencing initialization")
                    return
                }

                // Load user data
                val user = userRepository.getUser(currentUser.uid).getOrNull()
                if (user == null) {
                    Log.w(TAG, "Could not load user data")
                    return
                }

                // Check if user has configured home location
                if (!user.hasHomeLocation) {
                    Log.d(TAG, "User has no home location configured, skipping geofencing")
                    return
                }

                // Check permissions
                if (!locationPermissionManager.hasAllRequiredPermissions()) {
                    Log.d(TAG, "Missing location permissions, skipping geofencing")
                    cleanupGeofencingOnLogout()
                    return
                }

                // Check if geofencing is already active
                if (geofencingManager.isGeofenceActive()) {
                    Log.d(TAG, "Geofencing already active")
                    return
                }

                // Initialize geofencing
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
         * Cleans up geofencing on logout
         */
        fun cleanupGeofencingOnLogout() {
            Log.d(TAG, "Cleaning up geofencing on logout...")
            CoroutineScope(Dispatchers.IO).launch {
                try {
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
    }
