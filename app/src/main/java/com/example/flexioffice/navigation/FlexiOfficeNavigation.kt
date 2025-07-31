package com.example.flexioffice.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.flexioffice.presentation.components.TeamAccessRequired
import com.example.flexioffice.presentation.screens.BookingScreen
import com.example.flexioffice.presentation.screens.CalendarScreen
import com.example.flexioffice.presentation.screens.GeofencingSettingsScreen
import com.example.flexioffice.presentation.screens.LoginScreen
import com.example.flexioffice.presentation.screens.ProfileScreen
import com.example.flexioffice.presentation.screens.RequestsScreen
import com.example.flexioffice.presentation.screens.TeamsScreen

@Composable
fun FlexiOfficeNavigation(
    navController: NavHostController,
    startDestination: String,
    mainViewModel: com.example.flexioffice.presentation.MainViewModel = hiltViewModel(),
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
                },
            )
        }

        // Calendar Screen
        composable(FlexiOfficeRoutes.Calendar.route) {
            CalendarScreen(
                hiltViewModel(),
                navController,
            )
        }

        // Booking Screen - Protected by team membership
        composable(route = FlexiOfficeRoutes.Booking.route) {
            if (mainViewModel.hasAccessToRoute(FlexiOfficeRoutes.Booking.route)) {
                BookingScreen()
            } else {
                TeamAccessRequired(
                    onNavigateToTeams = {
                        navController.navigate(FlexiOfficeRoutes.Teams.route) {
                            popUpTo(FlexiOfficeRoutes.Booking.route) { inclusive = true }
                        }
                    },
                )
            }
        }

        composable(
            route = FlexiOfficeRoutes.Booking.FULL_ROUTE,
            arguments =
                listOf(
                    androidx.navigation.navArgument("date") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                    },
                ),
        ) { backStackEntry ->
            if (mainViewModel.hasAccessToRoute(FlexiOfficeRoutes.Booking.route)) {
                val date = backStackEntry.arguments?.getString("date")
                BookingScreen(selectedDate = date)
            } else {
                // Show team access required screen
                TeamAccessRequired(
                    onNavigateToTeams = {
                        navController.navigate(FlexiOfficeRoutes.Teams.route) {
                            popUpTo(FlexiOfficeRoutes.Booking.route) { inclusive = true }
                        }
                    },
                )
            }
        }

        // Requests Screen (nur für Manager/Leads)
        composable(FlexiOfficeRoutes.Requests.route) {
            // Zugriffskontrolle wird in der Scaffold-Ebene behandelt
            RequestsScreen()
        }

        composable(FlexiOfficeRoutes.Loading.route) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        // Profile Screen
        composable(FlexiOfficeRoutes.Profile.route) {
            ProfileScreen(navController = navController)
        }

        // Teams Screen
        composable(FlexiOfficeRoutes.Teams.route) { TeamsScreen() }

        // Geofencing Settings Screen
        composable(FlexiOfficeRoutes.GeofencingSettings.route) {
            GeofencingSettingsScreen(hiltViewModel(), navigateBack = {
                navController.popBackStack()
            })
        }
    }
}
