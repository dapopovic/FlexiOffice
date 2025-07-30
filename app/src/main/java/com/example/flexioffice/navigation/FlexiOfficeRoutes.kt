package com.example.flexioffice.navigation

/** Navigation-Routen für FlexiOffice */
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

    object Requests : FlexiOfficeRoutes("requests") // Nur für Leads

    object Profile : FlexiOfficeRoutes("profile")

    object Teams : FlexiOfficeRoutes("teams")

    companion object {
        /** Standard-Route für neue Benutzer */
        const val DEFAULT_ROUTE = "calendar"

        /** Alle verfügbaren BottomBar-Routen */
        val bottomBarRoutes = listOf(Calendar, Booking, Teams, Requests)
    }
}
