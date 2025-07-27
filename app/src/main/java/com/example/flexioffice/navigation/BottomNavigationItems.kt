package com.example.flexioffice.navigation

import com.example.flexioffice.R
import com.example.flexioffice.data.model.User

/** Navigation-Item für BottomBar */
data class BottomNavigationItem(
    val route: String,
    val title: String,
    val selectedIconId: Int,
    val unselectedIconId: Int,
    val hasNews: Boolean = false,
    val badgeCount: Int? = null,
    val requiredRole: String? = null, // null = für alle Rollen verfügbar
)

/** Factory für BottomNavigation Items */
object BottomNavigationItems {
    val calendar =
        BottomNavigationItem(
            route = FlexiOfficeRoutes.Calendar.route,
            title = "Kalender",
            selectedIconId = R.drawable.calendar_month_24px_filled,
            unselectedIconId = R.drawable.calendar_month_24px,
        )

    val booking =
        BottomNavigationItem(
            route = FlexiOfficeRoutes.Booking.route,
            title = "Buchen",
            selectedIconId = R.drawable.book_24px_filled,
            unselectedIconId = R.drawable.book_24px,
        )

    val requests =
        BottomNavigationItem(
            route = FlexiOfficeRoutes.Requests.route,
            title = "Anfragen",
            selectedIconId = R.drawable.assignment_24px_filled,
            unselectedIconId = R.drawable.assignment_24px,
            requiredRole = User.ROLE_MANAGER, // Nur für Manager/Leads
        )
    
    val profile =
        BottomNavigationItem(
            route = FlexiOfficeRoutes.Profile.route,
            title = "Profil",
            selectedIconId = R.drawable.book_24px_filled, // Temporär - wird später durch person_24px ersetzt
            unselectedIconId = R.drawable.book_24px // Temporär - wird später durch person_24px ersetzt
        )

    /** Gibt die für eine Rolle verfügbaren Navigation-Items zurück */
    fun getItemsForRole(userRole: String): List<BottomNavigationItem> {
        val allItems = listOf(calendar, booking, requests, profile)

        return allItems.filter { item ->
            item.requiredRole == null ||
                item.requiredRole == userRole ||
                (item.requiredRole == User.ROLE_MANAGER && userRole == User.ROLE_ADMIN)
        }
    }
}
