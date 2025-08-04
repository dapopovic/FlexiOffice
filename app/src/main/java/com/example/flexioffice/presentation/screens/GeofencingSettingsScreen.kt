package com.example.flexioffice.presentation.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flexioffice.R
import com.example.flexioffice.presentation.GeofencingSettingsViewModel
import com.example.flexioffice.presentation.components.Header

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofencingSettingsScreen(
    viewModel: GeofencingSettingsViewModel = hiltViewModel(),
    navigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission launcher for Location permissions
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val allPermissionsGranted = permissions.values.all { it }
            if (allPermissionsGranted) {
                viewModel.onLocationPermissionsGranted()
            } else {
                viewModel.onLocationPermissionsDenied()
            }
        }

    // Show Error-Messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearErrorMessage()
        }
    }

    // Show Success-Messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Header(
                title = stringResource(R.string.geofencing_title),
                subtitle = stringResource(R.string.geofencing_subtitle),
                iconVector = Icons.Default.LocationOn,
                iconDescription = stringResource(R.string.geofencing_location_icon_desc),
                onBackPressed = navigateBack,
                hasBackButton = true,
            )

            // Location Permission Card
            LocationPermissionCard(
                hasLocationPermissions = uiState.hasLocationPermissions,
                hasBackgroundPermission = uiState.hasBackgroundLocationPermission,
                onRequestLocationPermissions = {
                    val permissions =
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    locationPermissionLauncher.launch(permissions)
                },
                onRequestBackgroundPermissions = {
                    val permissions =
                        arrayOf(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        )
                    locationPermissionLauncher.launch(permissions)
                },
            )

            // Home Location Card
            HomeLocationCard(
                hasLocationPermissions = uiState.hasLocationPermissions,
                hasHomeLocation = uiState.user?.hasHomeLocation == true,
                homeLatitude = uiState.user?.homeLatitude ?: 0.0,
                homeLongitude = uiState.user?.homeLongitude ?: 0.0,
                isLoading = uiState.isSettingHomeLocation,
                onSetCurrentLocation = viewModel::setCurrentLocationAsHome,
            )

            // Geofencing Toggle Card
            GeofencingToggleCard(
                isGeofencingEnabled = uiState.isGeofencingActive,
                canEnableGeofencing = uiState.canEnableGeofencing,
                isLoading = uiState.isTogglingGeofencing,
                onToggleGeofencing = { enabled ->
                    if (enabled) {
                        viewModel.enableGeofencing()
                    } else {
                        viewModel.disableGeofencing()
                    }
                },
            )

            // Info Card
            GeofencingInfoCard()
        }
    }
}

@Composable
private fun LocationPermissionCard(
    hasLocationPermissions: Boolean,
    hasBackgroundPermission: Boolean,
    onRequestLocationPermissions: () -> Unit,
    onRequestBackgroundPermissions: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (hasLocationPermissions && hasBackgroundPermission) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.security_24px),
                    contentDescription = stringResource(R.string.location_permissions_icon_desc),
                    tint =
                        if (hasLocationPermissions && hasBackgroundPermission) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                )
                Text(
                    text = stringResource(R.string.location_permissions_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text =
                    if (hasLocationPermissions && hasBackgroundPermission) {
                        stringResource(R.string.location_permissions_granted)
                    } else {
                        stringResource(R.string.location_permissions_required)
                    },
                style = MaterialTheme.typography.bodyMedium,
            )

            if (!hasLocationPermissions) {
                Button(
                    onClick = onRequestLocationPermissions,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.location_permissions_request))
                }
            } else if (!hasBackgroundPermission) {
                Button(
                    onClick = onRequestBackgroundPermissions,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.location_background_permission_request))
                }
            } else {
                Text(
                    text = stringResource(R.string.location_permissions_all_granted_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomeLocationCard(
    hasLocationPermissions: Boolean,
    hasHomeLocation: Boolean,
    homeLatitude: Double,
    homeLongitude: Double,
    isLoading: Boolean,
    onSetCurrentLocation: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.home_work_24px),
                    contentDescription = stringResource(R.string.home_location_icon_desc),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.home_location_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (hasHomeLocation) {
                Text(
                    text = stringResource(R.string.home_location_configured),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.home_location_coordinates, homeLatitude, homeLongitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Kein Zuhause-Standort konfiguriert",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = onSetCurrentLocation,
                enabled = !isLoading && hasLocationPermissions,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        if (hasHomeLocation) {
                            "Zuhause-Standort aktualisieren"
                        } else {
                            "Aktuellen Standort als Zuhause festlegen"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun GeofencingToggleCard(
    isGeofencingEnabled: Boolean,
    canEnableGeofencing: Boolean,
    isLoading: Boolean,
    onToggleGeofencing: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector =
                        ImageVector.vectorResource(
                            if (isGeofencingEnabled) {
                                R.drawable.notifications_active_24px
                            } else {
                                R.drawable.notifications_off_24px
                            },
                        ),
                    contentDescription = stringResource(R.string.geofencing_icon_desc),
                    tint =
                        if (isGeofencingEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                Text(
                    text = stringResource(R.string.geofencing_toggle_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text =
                    if (isGeofencingEnabled) {
                        stringResource(R.string.geofencing_enabled_desc)
                    } else {
                        stringResource(R.string.geofencing_disabled_desc)
                    },
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        if (isGeofencingEnabled) {
                            stringResource(
                                R.string.switch_on,
                            )
                        } else {
                            stringResource(R.string.switch_off)
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Switch(
                        checked = isGeofencingEnabled,
                        enabled = canEnableGeofencing,
                        onCheckedChange = onToggleGeofencing,
                    )
                }
            }

            if (!canEnableGeofencing) {
                Text(
                    text = stringResource(R.string.geofencing_toggle_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun GeofencingInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.info_24px),
                    contentDescription = stringResource(R.string.info_icon_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.geofencing_info_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = stringResource(R.string.geofencing_info_body).trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
            )
        }
    }
}
