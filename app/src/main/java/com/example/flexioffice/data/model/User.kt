package com.example.flexioffice.data.model

/** User data model for Firestore */
data class User(
    val name: String = "",
    val email: String = "",
    val role: String = "user", // Default role
    val teamId: String = "", // Empty string = no team
    val id: String = "", // Firestore document ID, optional
    // Firebase Cloud Messaging Token for push notifications
    val fcmToken: String = "",
    // Home Office coordinates for geofencing
    val homeLatitude: Double = 0.0,
    val homeLongitude: Double = 0.0,
    val hasHomeLocation: Boolean = false,
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "user", "", "", "", 0.0, 0.0, false)

    companion object {
        // Standard roles
        const val ROLE_USER = "user"
        const val ROLE_ADMIN = "admin"
        const val ROLE_MANAGER = "manager"

        // No team
        const val NO_TEAM = ""

        // const fields
        const val NAME_FIELD = "name"
        const val EMAIL_FIELD = "email"
        const val ROLE_FIELD = "role"
        const val TEAM_ID_FIELD = "teamId"
        const val ID_FIELD = "id"
        const val FCM_TOKEN_FIELD = "fcmToken"
        const val HOME_LATITUDE_FIELD = "homeLatitude"
        const val HOME_LONGITUDE_FIELD = "homeLongitude"
        const val HAS_HOME_LOCATION_FIELD = "hasHomeLocation"

        // collection name in Firestore
        const val COLLECTION_NAME = "users"
    }
}
