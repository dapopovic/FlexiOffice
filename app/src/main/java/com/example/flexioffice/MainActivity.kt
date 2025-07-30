package com.example.flexioffice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                // Zeige einen Ladebildschirm, während die Authentifizierung läuft
                navController.navigate(FlexiOfficeRoutes.Loading.route) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                }
            }
            uiState.isLoggedIn -> {
                // Wenn der Benutzer angemeldet ist, navigiere zur Startseite
                navController.navigate(FlexiOfficeRoutes.Calendar.route) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                }
            }
            else -> {
                // Wenn der Benutzer nicht angemeldet ist, navigiere zum Login
                navController.navigate(FlexiOfficeRoutes.Login.route) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                }
            }
        }
    }
    MainAppScreen(hiltViewModel(), navController, hiltViewModel())
}
