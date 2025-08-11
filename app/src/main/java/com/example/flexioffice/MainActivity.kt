package com.example.flexioffice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flexioffice.navigation.FlexiOfficeRoutes
import com.example.flexioffice.presentation.AuthViewModel
import com.example.flexioffice.presentation.screens.MainAppScreen
import com.example.flexioffice.ui.theme.FlexiOfficeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { FlexiOfficeTheme { MainFlexiOfficeApp() } }
    }
}

@Composable
fun MainFlexiOfficeApp(authViewModel: AuthViewModel = hiltViewModel()) {
    val uiState by authViewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    // First-launch: Onboarding check
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("flexioffice_prefs", Context.MODE_PRIVATE)
        val seen = prefs.getBoolean("onboarding_completed", false)
        if (!seen) {
            navController.navigate(FlexiOfficeRoutes.Onboarding.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            }
        }
    }

    // Show first-launch notifications prompt only if the user is logged in
    // (Android 13+ will request permission). This avoids prompting on the login screen.
    val currentBackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackEntry?.destination?.route

    if (uiState.isLoggedIn && currentRoute != FlexiOfficeRoutes.Onboarding.route) {
        NotificationsPermissionPrompt()
    }

    LaunchedEffect(uiState.isLoading, uiState.isLoggedIn) {
        when {
            uiState.isLoading -> {
                // Show loading screen while checking auth state
                val currentRoute = navController.currentDestination?.route
                if (currentRoute != FlexiOfficeRoutes.Onboarding.route) {
                    navController.navigate(FlexiOfficeRoutes.Loading.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            }
            uiState.isLoggedIn -> {
                // Only navigate to Calendar if we're currently on Login or Loading screen
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == FlexiOfficeRoutes.Login.route ||
                    currentRoute == FlexiOfficeRoutes.Loading.route ||
                    currentRoute == null
                ) {
                    navController.navigate(FlexiOfficeRoutes.Calendar.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            }
            else -> {
                // If the user is not logged in, navigate to the login screen
                val currentRoute = navController.currentDestination?.route
                // Don't override onboarding if it's being shown
                if (currentRoute != FlexiOfficeRoutes.Onboarding.route) {
                    navController.navigate(FlexiOfficeRoutes.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            }
        }
    }
    MainAppScreen(hiltViewModel(), navController, hiltViewModel())
}

@Composable
private fun NotificationsPermissionPrompt() {
    val context = androidx.compose.ui.platform.LocalContext.current

    // SharedPreferences to remember if we've shown the prompt already
    val prefs = remember { context.getSharedPreferences("flexioffice_prefs", Context.MODE_PRIVATE) }
    val promptShownKey = "notifications_prompt_shown"
    val alreadyShown = remember { mutableStateOf(prefs.getBoolean(promptShownKey, false)) }

    // Check if permission is needed (Android 13+)
    val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val hasPermission =
        if (needsRuntimePermission) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // On older Android versions, notifications are enabled by default (unless user disabled at OS level)
            true
        }

    val shouldShowDialog =
        remember(alreadyShown.value, hasPermission) {
            !alreadyShown.value && !hasPermission
        }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { _ ->
                // Regardless of result, don't nag again on next app start
                prefs.edit { putBoolean(promptShownKey, true) }
                alreadyShown.value = true
            },
        )

    if (shouldShowDialog) {
        AlertDialog(
            onDismissRequest = {
                prefs.edit { putBoolean(promptShownKey, true) }
                alreadyShown.value = true
            },
            title = { Text(text = stringResource(id = R.string.notifications_permission_title)) },
            text = { Text(text = stringResource(id = R.string.notifications_permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    if (needsRuntimePermission) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Nothing to request on < 33, just mark as shown
                        prefs.edit { putBoolean(promptShownKey, true) }
                        alreadyShown.value = true
                    }
                }) {
                    Text(text = stringResource(id = R.string.notifications_permission_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    prefs.edit { putBoolean(promptShownKey, true) }
                    alreadyShown.value = true
                }) {
                    Text(text = stringResource(id = R.string.notifications_permission_later))
                }
            },
        )
    }
}
