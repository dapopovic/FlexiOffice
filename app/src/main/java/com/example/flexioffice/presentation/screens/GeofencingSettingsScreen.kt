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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flexioffice.R
import com.example.flexioffice.presentation.GeofencingSettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofencingSettingsScreen(
    viewModel: GeofencingSettingsViewModel = hiltViewModel(),
    navigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission launcher für Location-Berechtigungen
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

    // Zeige Error-Messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearErrorMessage()
        }
    }

    // Zeige Success-Messages
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
            // Header
            GeofencingSettingsHeader(
                onBackPressed = navigateBack,
            )

            // Location Permission Card
            LocationPermissionCard(
                hasLocationPermissions = uiState.hasLocationPermissions,
                hasBackgroundPermission = uiState.hasBackgroundLocationPermission,
                onRequestPermissions = {
                    val permissions =
                        arrayOf(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        )
                    locationPermissionLauncher.launch(permissions)
                },
            )

            // Home Location Card
            HomeLocationCard(
                hasHomeLocation = uiState.user?.hasHomeLocation == true,
                homeLatitude = uiState.user?.homeLatitude ?: 0.0,
                homeLongitude = uiState.user?.homeLongitude ?: 0.0,
                isLoading = uiState.isSettingHomeLocation,
                onSetCurrentLocation = {
                    if (uiState.hasLocationPermissions) {
                        viewModel.setCurrentLocationAsHome()
                    } else {
                        // Request permissions first
                        val permissions =
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        locationPermissionLauncher.launch(permissions)
                    }
                },
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
private fun GeofencingSettingsHeader(onBackPressed: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(
            onClick = onBackPressed,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Location Icon",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
        Column {
            Text(
                text = "Home Office Erinnerungen",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Automatische Benachrichtigungen beim Verlassen des Zuhauses",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LocationPermissionCard(
    hasLocationPermissions: Boolean,
    hasBackgroundPermission: Boolean,
    onRequestPermissions: () -> Unit,
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
                    contentDescription = "Berechtigungen",
                    tint =
                        if (hasLocationPermissions && hasBackgroundPermission) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                )
                Text(
                    text = "Standort-Berechtigungen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text =
                    if (hasLocationPermissions && hasBackgroundPermission) {
                        "✅ Alle erforderlichen Berechtigungen erteilt"
                    } else {
                        "❌ Standort-Berechtigungen erforderlich für Home Office Erinnerungen"
                    },
                style = MaterialTheme.typography.bodyMedium,
            )

            if (!hasLocationPermissions || !hasBackgroundPermission) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Berechtigungen erteilen")
                }
            }
        }
    }
}

@Composable
private fun HomeLocationCard(
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
                    contentDescription = "Zuhause",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Zuhause-Standort",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (hasHomeLocation) {
                Text(
                    text = "✅ Zuhause-Standort konfiguriert",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Koordinaten: ${String.format(
                        Locale.getDefault(),
                        "%.6f",
                        homeLatitude,
                    )}, ${String.format(Locale.getDefault(),"%.6f", homeLongitude)}",
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
                enabled = !isLoading,
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
                    contentDescription = "Geofencing",
                    tint =
                        if (isGeofencingEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                Text(
                    text = "Home Office Erinnerungen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text =
                    if (isGeofencingEnabled) {
                        "Sie erhalten eine Benachrichtigung, wenn Sie bei geplantem Home Office das Zuhause verlassen."
                    } else {
                        "Aktivieren Sie Erinnerungen für Home Office Tage."
                    },
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isGeofencingEnabled) "Aktiviert" else "Deaktiviert",
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
                    text = "⚠️ Berechtigungen und Zuhause-Standort erforderlich",
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
                    contentDescription = "Information",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Wie funktioniert es?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text =
                    """
                    • Die App überwacht einen 200-Meter-Radius um Ihr Zuhause
                    • Bei geplantem Home Office erhalten Sie eine Erinnerung, wenn Sie das Zuhause verlassen
                    • Die Benachrichtigung wird nur einmal pro Tag gesendet
                    • Ihre Standortdaten werden nur lokal auf dem Gerät gespeichert
                    """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
            )
        }
    }
}
