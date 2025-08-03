package com.example.flexioffice.data.model

/** Data class for teams in Firestore */
data class Team(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val members: List<String> = emptyList(), // List of user UIDs
    val managerId: String = "", // UID of the team manager
) {
    // Empty constructor for Firestore
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
