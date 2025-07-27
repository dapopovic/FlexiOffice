package com.example.flexioffice.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.flexioffice.presentation.screens.BookingScreen
import com.example.flexioffice.presentation.screens.CalendarScreen
import com.example.flexioffice.presentation.screens.LoginScreen
import com.example.flexioffice.presentation.screens.ProfileScreen
import com.example.flexioffice.presentation.screens.RequestsScreen

@Composable
fun FlexiOfficeNavigation(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize(),
    ) {
        // Authentication Screen
        composable(FlexiOfficeRoutes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Navigation wird durch MainViewModel in MainActivity behandelt
                },
            )
        }

        // Calendar Screen
        composable(FlexiOfficeRoutes.Calendar.route) { CalendarScreen() }

        // Booking Screen
        composable(FlexiOfficeRoutes.Booking.route) { BookingScreen() }

        // Requests Screen (nur für Manager/Leads)
        composable(FlexiOfficeRoutes.Requests.route) {
            // Zugriffskontrolle wird in der Scaffold-Ebene behandelt
            RequestsScreen()
        }

        // Profile Screen (für zukünftige Implementierung)
        composable(FlexiOfficeRoutes.Profile.route) { ProfileScreen() }
    }
}
