package com.example.flexioffice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
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

    LaunchedEffect(uiState.isLoading, uiState.isLoggedIn) {
        when {
            uiState.isLoading -> {
                // Show loading screen while checking auth state
                navController.navigate(FlexiOfficeRoutes.Loading.route) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
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
                navController.navigate(FlexiOfficeRoutes.Login.route) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                }
            }
        }
    }
    MainAppScreen(hiltViewModel(), navController, hiltViewModel())
}
