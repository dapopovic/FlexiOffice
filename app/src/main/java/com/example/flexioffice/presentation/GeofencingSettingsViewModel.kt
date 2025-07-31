package com.example.flexioffice.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.data.model.User
import com.example.flexioffice.geofencing.GeofencingManager
import com.example.flexioffice.geofencing.permissions.LocationPermissionManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class GeofencingSettingsUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val hasLocationPermissions: Boolean = false,
    val hasBackgroundLocationPermission: Boolean = false,
    val isGeofencingActive: Boolean = false,
    val isSettingHomeLocation: Boolean = false,
    val isTogglingGeofencing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val canEnableGeofencing: Boolean
        get() =
            hasLocationPermissions &&
                hasBackgroundLocationPermission &&
                user?.hasHomeLocation == true
}

@HiltViewModel
class GeofencingSettingsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val geofencingManager: GeofencingManager,
        private val locationPermissionManager: LocationPermissionManager,
    ) : ViewModel() {
        companion object {
            private const val TAG = "GeofencingSettingsViewModel"
        }

        private val _uiState = MutableStateFlow(GeofencingSettingsUiState())
        val uiState: StateFlow<GeofencingSettingsUiState> = _uiState.asStateFlow()

        private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        init {
            observeUserAndPermissions()
        }

        private fun observeUserAndPermissions() {
            viewModelScope.launch {
                authRepository.currentUser
                    .catch { e ->
                        Log.e(TAG, "Error observing user", e)
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Fehler beim Laden der Benutzerdaten: ${e.message}",
                            )
                    }.collect { firebaseUser ->
                        val user =
                            if (firebaseUser?.uid != null) {
                                userRepository.getUser(firebaseUser.uid).getOrNull()
                            } else {
                                null
                            }
                        updatePermissionsAndState(user)
                    }
            }
        }

        private fun updatePermissionsAndState(user: User?) {
            val hasLocationPermissions = locationPermissionManager.hasBasicLocationPermissions()
            val hasBackgroundPermission = locationPermissionManager.hasBackgroundLocationPermission()
            val isGeofencingActive = geofencingManager.isGeofenceActive()

            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    user = user,
                    hasLocationPermissions = hasLocationPermissions,
                    hasBackgroundLocationPermission = hasBackgroundPermission,
                    isGeofencingActive = isGeofencingActive,
                )
        }

        fun onLocationPermissionsGranted() {
            Log.d(TAG, "Location permissions granted")
            updatePermissionsAndState(_uiState.value.user)

            _uiState.value =
                _uiState.value.copy(
                    successMessage = "Standort-Berechtigungen erfolgreich erteilt",
                )
        }

        fun onLocationPermissionsDenied() {
            Log.d(TAG, "Location permissions denied")
            updatePermissionsAndState(_uiState.value.user)

            _uiState.value =
                _uiState.value.copy(
                    errorMessage = "Standort-Berechtigungen sind für Home Office Erinnerungen erforderlich",
                )
        }

        @SuppressLint("MissingPermission")
        fun setCurrentLocationAsHome() {
            if (!locationPermissionManager.hasBasicLocationPermissions()) {
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = "Standort-Berechtigungen erforderlich",
                    )
                return
            }

            val currentUser = _uiState.value.user
            if (currentUser == null) {
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = "Benutzer nicht gefunden",
                    )
                return
            }

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isSettingHomeLocation = true)

                try {
                    // Hole aktuellen Standort
                    val cancellationTokenSource = CancellationTokenSource()
                    viewModelScope.launch {
                        delay(30000)
                        cancellationTokenSource.cancel() // Abbrechen nach 30 Sekunden
                        _uiState.value =
                            _uiState.value.copy(
                                isSettingHomeLocation = false,
                                errorMessage = "Standortabfrage abgebrochen. Bitte erneut versuchen.",
                            )
                    }
                    val location =
                        fusedLocationClient
                            .getCurrentLocation(
                                Priority.PRIORITY_HIGH_ACCURACY,
                                cancellationTokenSource.token,
                            ).await()

                    if (location != null) {
                        // Aktualisiere User in Firestore
                        userRepository
                            .updateUserHomeLocation(
                                uid = currentUser.id,
                                latitude = location.latitude,
                                longitude = location.longitude,
                            ).fold(
                                onSuccess = {
                                    Log.d(
                                        TAG,
                                        "Home location successfully updated: ${location.latitude}, ${location.longitude}",
                                    )

                                    // Lade User-Daten neu
                                    val updatedUser = userRepository.getUser(currentUser.id).getOrNull()
                                    _uiState.value =
                                        _uiState.value.copy(
                                            user = updatedUser,
                                            successMessage = "Zuhause-Standort erfolgreich festgelegt",
                                            isSettingHomeLocation = false,
                                        )
                                },
                                onFailure = { error ->
                                    Log.e(TAG, "Failed to update home location", error)
                                    _uiState.value =
                                        _uiState.value.copy(
                                            errorMessage = "Fehler beim Speichern des Standorts: ${error.message}",
                                            isSettingHomeLocation = false,
                                        )
                                },
                            )
                    } else {
                        _uiState.value =
                            _uiState.value.copy(
                                errorMessage = "Standort konnte nicht ermittelt werden. Versuchen Sie es erneut.",
                                isSettingHomeLocation = false,
                            )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting current location", e)
                    _uiState.value =
                        _uiState.value.copy(
                            errorMessage = "Fehler beim Abrufen des Standorts: ${e.message}",
                            isSettingHomeLocation = false,
                        )
                }
            }
        }

        fun enableGeofencing() {
            val currentUser = _uiState.value.user
            if (currentUser == null) {
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = "Benutzer nicht gefunden",
                    )
                return
            }

            if (!_uiState.value.canEnableGeofencing) {
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = "Berechtigungen und Zuhause-Standort erforderlich",
                    )
                return
            }

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isTogglingGeofencing = true)

                geofencingManager.setupHomeGeofence(currentUser).fold(
                    onSuccess = {
                        Log.d(TAG, "Geofencing successfully enabled")
                        _uiState.value =
                            _uiState.value.copy(
                                isGeofencingActive = true,
                                isTogglingGeofencing = false,
                                successMessage = "Home Office Erinnerungen aktiviert",
                            )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to enable geofencing", error)
                        _uiState.value =
                            _uiState.value.copy(
                                isTogglingGeofencing = false,
                                errorMessage = "Fehler beim Aktivieren der Erinnerungen: ${error.message}",
                            )
                    },
                )
            }
        }

        fun disableGeofencing() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isTogglingGeofencing = true)

                geofencingManager.removeGeofences().fold(
                    onSuccess = {
                        Log.d(TAG, "Geofencing successfully disabled")
                        _uiState.value =
                            _uiState.value.copy(
                                isGeofencingActive = false,
                                isTogglingGeofencing = false,
                                successMessage = "Home Office Erinnerungen deaktiviert",
                            )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to disable geofencing", error)
                        _uiState.value =
                            _uiState.value.copy(
                                isTogglingGeofencing = false,
                                errorMessage = "Fehler beim Deaktivieren der Erinnerungen: ${error.message}",
                            )
                    },
                )
            }
        }

        fun clearErrorMessage() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }

        fun clearSuccessMessage() {
            _uiState.value = _uiState.value.copy(successMessage = null)
        }
    }
