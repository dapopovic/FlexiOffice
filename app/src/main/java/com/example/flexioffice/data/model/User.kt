package com.example.flexioffice.data.model

/** Benutzer-Datenmodell für Firestore */
data class User(
    val name: String = "",
    val email: String = "",
    val role: String = "user", // Standard-Rolle
    val teamId: String = "", // Leerer String = kein Team
    val id: String = "", // Firestore-Dokument-ID, optional
) {
    // Leerer Konstruktor für Firestore
    constructor() : this("", "", "user", "", "")

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

        // collection name in Firestore
        const val COLLECTION_NAME = "users"
    }
}
