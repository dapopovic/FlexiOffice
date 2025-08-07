package com.example.flexioffice.data.model

import java.util.Date

/** Team invitation data model for Firestore */
data class TeamInvitation(
    val id: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val invitedByUserId: String = "",
    val invitedByUserName: String = "",
    val invitedUserEmail: String = "",
    val invitedUserId: String = "",
    val status: String = STATUS_PENDING,
    val createdAt: Date = Date(),
    val respondedAt: Date? = null,
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", "", "", "", "", STATUS_PENDING, Date(), null)

    companion object {
        // Invitation status
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_DECLINED = "declined"

        // Firestore field names
        const val ID_FIELD = "id"
        const val TEAM_ID_FIELD = "teamId"
        const val TEAM_NAME_FIELD = "teamName"
        const val INVITED_BY_USER_ID_FIELD = "invitedByUserId"
        const val INVITED_BY_USER_NAME_FIELD = "invitedByUserName"
        const val INVITED_USER_EMAIL_FIELD = "invitedUserEmail"
        const val INVITED_USER_ID_FIELD = "invitedUserId"
        const val STATUS_FIELD = "status"
        const val CREATED_AT_FIELD = "createdAt"
        const val RESPONDED_AT_FIELD = "respondedAt"

        // Firestore collection name
        const val COLLECTION_NAME = "teamInvitations"
    }
}
