package com.example.flexioffice.data.model

/** Benutzer-Datenmodell für Firestore */
data class User(
    val name: String = "",
    val email: String = "",
    val role: String = "user", // Standard-Rolle
    val teamId: String = "", // Leerer String = kein Team
    val id: String = "", // Firestore-Dokument-ID, optional
    // Firebase Cloud Messaging Token für Push-Benachrichtigungen
    val fcmToken: String = "",
    // Home Office Koordinaten für Geofencing
    val homeLatitude: Double = 0.0,
    val homeLongitude: Double = 0.0,
    val hasHomeLocation: Boolean = false,
) {
    // Leerer Konstruktor für Firestore
    constructor() : this("", "", "user", "", "", "", 0.0, 0.0, false)

    companion object {
        // Standard-Rollen
        const val ROLE_USER = "user"
        const val ROLE_ADMIN = "admin"
        const val ROLE_MANAGER = "manager"

        // Kein Team
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
