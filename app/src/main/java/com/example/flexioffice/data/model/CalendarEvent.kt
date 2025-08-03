package com.example.flexioffice.data.model

import com.example.flexioffice.R
import java.time.LocalDate

/** Calendar event for UI representation */
data class CalendarEvent(
    val id: String,
    val title: String,
    val date: LocalDate,
    val type: EventType,
    val participantNames: List<String> = emptyList(),
    val color: Int = R.color.status_unknown, // Default blue color
    val status: BookingStatus = BookingStatus.PENDING, // Booking status for icon consistency
)

enum class EventType {
    HOME_OFFICE,
    TEAM_MEETING,
    OFFICE_BOOKING,
    VACATION,
}
