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
    val requiresTeamMembership: Boolean = false, // true = erfordert Team-Mitgliedschaft
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
            requiresTeamMembership = true, // Erfordert Team-Mitgliedschaft
        )

    val requests =
        BottomNavigationItem(
            route = FlexiOfficeRoutes.Requests.route,
            title = "Anfragen",
            selectedIconId = R.drawable.assignment_24px_filled,
            unselectedIconId = R.drawable.assignment_24px,
            requiredRole = User.ROLE_MANAGER, // Nur für Manager/Leads
        )

    val teams =
        BottomNavigationItem(
            route = FlexiOfficeRoutes.Teams.route,
            title = "Teams",
            selectedIconId = R.drawable.group_24px_filled,
            unselectedIconId = R.drawable.group_24px,
        )

    val profile =
        BottomNavigationItem(
            route = FlexiOfficeRoutes.Profile.route,
            title = "Profil",
            selectedIconId = R.drawable.person_24px_filled,
            unselectedIconId = R.drawable.person_24px,
        )

    /** Gibt die für eine Rolle und Team-Status verfügbaren Navigation-Items zurück */
    fun getItemsForUser(user: User?): List<BottomNavigationItem> {
        val allItems = listOf(calendar, booking, requests, teams, profile)
        val userRole = user?.role ?: User.ROLE_USER
        val hasTeam = user?.teamId?.isNotEmpty() == true && user.teamId != User.NO_TEAM

        return allItems.filter { item ->
            // Prüfe Rollen-Berechtigung
            val hasRoleAccess =
                item.requiredRole == null ||
                    item.requiredRole == userRole ||
                    (item.requiredRole == User.ROLE_MANAGER && userRole == User.ROLE_ADMIN)

            // Prüfe Team-Mitgliedschaft falls erforderlich
            val hasTeamAccess = !item.requiresTeamMembership || hasTeam

            hasRoleAccess && hasTeamAccess
        }
    }
}
