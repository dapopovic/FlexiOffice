package com.example.flexioffice.data.model

/** Datenmodell für Teams in Firestore */
data class Team(
    val name: String = "",
    val description: String = "",
    val members: List<String> = emptyList(), // Liste von User-UIDs
    val managerId: String = "", // UID des Team-Managers
) {
    // Leerer Konstruktor für Firestore
    constructor() : this("", "", emptyList(), "")
}
