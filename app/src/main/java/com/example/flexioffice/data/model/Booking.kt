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
    val status: BookingStatus = BookingStatus.APPROVED,
    val comment: String = "",
    val createdAt: String = "",
    val reviewerId: String = "",
) {
    // Leerer Konstruktor für Firestore
    constructor() :
        this("", "", "", "", "", BookingType.HOME_OFFICE, BookingStatus.APPROVED, "", "", "")

    @get:Exclude @set:Exclude
    var date: LocalDate
        get() = LocalDate.parse(this.dateString)
        set(value) {
            this.dateString = value.toString()
        }
}

enum class BookingType {
    HOME_OFFICE,
    OFFICE_DESK,
    MEETING_ROOM,
    PHONE_BOOTH,
}

enum class BookingStatus {
    PENDING,
    APPROVED,
    DECLINED,
}
