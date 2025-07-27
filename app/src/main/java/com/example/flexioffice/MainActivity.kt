package com.example.flexioffice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flexioffice.presentation.MainViewModel
import com.example.flexioffice.presentation.screens.LoginScreen
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
fun MainFlexiOfficeApp(mainViewModel: MainViewModel = hiltViewModel()) {
    val uiState by mainViewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            // Loading-Zustand
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.isLoggedIn -> {
            // Benutzer ist angemeldet - zeige Hauptapp mit Navigation
            MainAppScreen()
        }
        else -> {
            // Benutzer ist nicht angemeldet - zeige Login
            LoginScreen(
                onLoginSuccess = { // Navigation wird automatisch durch MainViewModel behandelt
                },
            )
        }
    }
}
