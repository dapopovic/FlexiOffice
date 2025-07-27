package com.example.flexioffice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flexioffice.presentation.AuthViewModel
import com.example.flexioffice.presentation.screens.HomeScreen
import com.example.flexioffice.presentation.screens.LoginScreen
import com.example.flexioffice.ui.theme.FlexiOfficeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FlexiOfficeTheme { FlexiOfficeApp() } }
    }
}

@Composable
fun FlexiOfficeApp(authViewModel: AuthViewModel = hiltViewModel()) {
    val uiState by authViewModel.uiState.collectAsState()

    if (uiState.isLoggedIn) {
        HomeScreen(onLogout = { /* Navigation wird automatisch durch AuthViewModel behandelt */})
    } else {
        LoginScreen(
                onLoginSuccess = { /* Navigation wird automatisch durch AuthViewModel behandelt */}
        )
    }
}
