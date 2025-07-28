package com.example.flexioffice.data.model

import java.time.LocalDate

/** Datenmodell für Home-Office-Buchungen in Firestore */
data class HomeOfficeBooking(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val teamId: String = "",
    val date: String = "", // ISO format: yyyy-MM-dd
    val type: BookingType = BookingType.HOME_OFFICE,
    val status: BookingStatus = BookingStatus.APPROVED,
    val comment: String = "",
    val createdAt: String = "",
    val reviewerId: String = "",
) {
    // Leerer Konstruktor für Firestore
    constructor() :
        this("", "", "", "", "", BookingType.HOME_OFFICE, BookingStatus.APPROVED, "", "", "")

    /** Konvertiert das String-Datum zu LocalDate */
    fun getLocalDate(): LocalDate? =
        try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            null
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
