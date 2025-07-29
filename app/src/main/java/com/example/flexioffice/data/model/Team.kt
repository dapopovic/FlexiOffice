package com.example.flexioffice.data.model

/** Datenmodell für Teams in Firestore */
data class Team(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val members: List<String> = emptyList(), // Liste von User-UIDs
    val managerId: String = "", // UID des Team-Managers
) {
    // Leerer Konstruktor für Firestore
    constructor() : this("", "", "", emptyList(), "")

    companion object {
        const val ID_FIELD = "id"
        const val NAME_FIELD = "name"
        const val DESCRIPTION_FIELD = "description"
        const val MEMBERS_FIELD = "members"
        const val MANAGER_ID_FIELD = "managerId"

        // collection name in Firestore
        const val COLLECTION_NAME = "teams"
    }
}
