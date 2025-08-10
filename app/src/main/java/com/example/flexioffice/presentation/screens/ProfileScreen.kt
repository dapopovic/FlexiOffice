package com.example.flexioffice.presentation.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.flexioffice.R
import com.example.flexioffice.navigation.FlexiOfficeRoutes
import com.example.flexioffice.presentation.AuthViewModel
import com.example.flexioffice.presentation.MainViewModel
import com.example.flexioffice.presentation.components.AppInformationCard
import com.example.flexioffice.presentation.components.Header
import com.example.flexioffice.presentation.components.ProfileActions
import com.example.flexioffice.presentation.components.UserInformationCard

private const val TAG = "ProfileScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
    navController: NavHostController? = null,
) {
    val authUiState by authViewModel.uiState.collectAsState()
    val mainUiState by mainViewModel.uiState.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Header(
            title = stringResource(R.string.profile_title),
            iconVector = Icons.Default.Person,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // User information
        UserInformationCard(
            user = mainUiState.currentUser,
            firebaseUser = authUiState.user,
        )

        // App information
        AppInformationCard()

        NotificationsCard()

        // Profile Actions (Geofencing Settings and Logout buttons)
        ProfileActions(
            isLoading = authUiState.isLoading,
            onGeofencingSettingsClick = {
                navController?.navigate(FlexiOfficeRoutes.GeofencingSettings.route)
            },
            onLogoutClick = {
                authViewModel.signOut()
            },
        )

        Spacer(modifier = Modifier.weight(1f))

        // Footer
        Text(
            text = stringResource(R.string.footer_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NotificationsCard() {
    val context = LocalContext.current
    val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun hasNotificationsPermission(ctx: Context): Boolean =
        if (needsRuntimePermission) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    val (granted, setGranted) = remember { mutableStateOf(hasNotificationsPermission(context)) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { grantedNow -> setGranted(grantedNow) },
        )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.notifications_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Text(
                text = stringResource(id = R.string.notifications_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text =
                    if (granted) {
                        stringResource(
                            id = R.string.notifications_status_enabled,
                        )
                    } else {
                        stringResource(id = R.string.notifications_status_disabled)
                    },
                style = MaterialTheme.typography.bodyMedium,
            )

            if (!granted && needsRuntimePermission) {
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(text = stringResource(id = R.string.notifications_enable_button))
                }
            } else {
                Button(
                    onClick = {
                        val intent =
                            Intent().apply {
                                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                // Some devices expect this extra
                                putExtra("app_package", context.packageName)
                                putExtra("app_uid", context.applicationInfo.uid)
                            }
                        // Fallback for older versions
                        if (intent.resolveActivity(context.packageManager) == null) {
                            val uri = ("package:" + context.packageName).toUri()
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
                        } else {
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(text = stringResource(id = R.string.notifications_open_settings_button))
                }
            }
        }
    }
}
