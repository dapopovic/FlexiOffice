package com.example.flexioffice.navigation

/** Navigation-Routes for FlexiOffice */
sealed class FlexiOfficeRoutes(
    val route: String,
) {
    // Auth Routes
    object Login : FlexiOfficeRoutes("login")

    object Loading : FlexiOfficeRoutes("loading")

    // Main App Routes
    object Calendar : FlexiOfficeRoutes("calendar")

    object Booking : FlexiOfficeRoutes("booking") {
        const val FULL_ROUTE = "booking?date={date}"
    }

    object Requests : FlexiOfficeRoutes("requests") // Only for leads

    object Profile : FlexiOfficeRoutes("profile")

    object Teams : FlexiOfficeRoutes("teams")

    object GeofencingSettings : FlexiOfficeRoutes("geofencing_settings")

    companion object {
        /** Standard-Route for new users */
        const val DEFAULT_ROUTE = "calendar"

        /** All available BottomBar routes */
        val bottomBarRoutes = listOf(Calendar, Booking, Teams, Requests)
    }
}
