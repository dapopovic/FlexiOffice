package com.example.flexioffice.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.time.LocalDate

/** Datenmodell für Home-Office-Buchungen in Firestore */
data class Booking(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val teamId: String = "",
    @get:PropertyName("date") @set:PropertyName("date")
    var dateString: String = LocalDate.now().toString(), // ISO format: yyyy-MM-dd
    val type: BookingType = BookingType.HOME_OFFICE,
    val status: BookingStatus = BookingStatus.PENDING,
    val comment: String = "",
    val createdAt: String = "",
    val reviewerId: String = "",
) {
    // Leerer Konstruktor für Firestore
    constructor() : this("", "", "", "", "", BookingType.HOME_OFFICE, BookingStatus.PENDING, "", "", "")

    @get:Exclude @set:Exclude
    var date: LocalDate
        get() = LocalDate.parse(this.dateString)
        set(value) {
            this.dateString = value.toString()
        }

    companion object {
        const val ID_FIELD = "id"
        const val USER_ID_FIELD = "userId"
        const val TEAM_ID_FIELD = "teamId"
        const val DATE_FIELD = "date"
        const val TYPE_FIELD = "type"
        const val STATUS_FIELD = "status"
        const val COMMENT_FIELD = "comment"
        const val CREATED_AT_FIELD = "createdAt"
        const val REVIEWER_ID_FIELD = "reviewerId"
        const val USER_NAME_FIELD = "userName"

        // collection name in Firestore
        const val COLLECTION_NAME = "bookings"
    }
}

enum class BookingType {
    HOME_OFFICE,
}

enum class BookingStatus {
    PENDING,
    APPROVED,
    DECLINED,
    CANCELLED,
}
