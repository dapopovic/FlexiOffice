package com.example.flexioffice.data.model

/** Benutzer-Datenmodell für Firestore */
data class User(
    val name: String = "",
    val email: String = "",
    val role: String = "user", // Standard-Rolle
    // -1 = nicht initialisiert
    val teamId: Int = -1,
) {
    // Leerer Konstruktor für Firestore
    constructor() : this("", "", "user", -1)

    companion object {
        // Standard-Rollen
        const val ROLE_USER = "user"
        const val ROLE_ADMIN = "admin"
        const val ROLE_MANAGER = "manager"

        // Nicht initialisierte Team-ID
        const val TEAM_NOT_INITIALIZED = -1
    }
}
