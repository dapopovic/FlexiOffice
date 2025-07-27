package com.example.flexioffice.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flexioffice.presentation.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onLogout: () -> Unit, authViewModel: AuthViewModel = hiltViewModel()) {
    val uiState by authViewModel.uiState.collectAsState()

    // Navigation nach Logout
    LaunchedEffect(uiState.isLoggedIn) {
        if (!uiState.isLoggedIn) {
            onLogout()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
                title = { Text("FlexiOffice") },
                actions = {
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Abmelden")
                    }
                }
        )

        // Main Content
        Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Text(
                    text = "Willkommen bei FlexiOffice!",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
            )

            uiState.user?.let { user ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                text = "Angemeldet als:",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                                text = user.email ?: "Unbekannte E-Mail",
                                style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }

            Text(
                    text = "Session wird automatisch nach App-Neustart wiederhergestellt.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(onClick = { authViewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                )
                Text("Abmelden")
            }
        }
    }
}
