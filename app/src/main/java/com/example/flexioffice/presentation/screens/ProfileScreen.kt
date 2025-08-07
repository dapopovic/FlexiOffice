package com.example.flexioffice.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                .padding(16.dp),
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
