package com.example.flexioffice.data.model

import java.time.LocalDate

/** Kalender-Event für die UI-Darstellung */
data class CalendarEvent(
    val id: String,
    val title: String,
    val date: LocalDate,
    val type: EventType,
    val participantNames: List<String> = emptyList(),
    val color: Long = 0xFF2196F3, // Default blue color
)

enum class EventType {
    HOME_OFFICE,
    TEAM_MEETING,
    OFFICE_BOOKING,
    VACATION,
}
