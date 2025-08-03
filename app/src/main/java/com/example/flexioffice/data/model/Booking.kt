package com.example.flexioffice.data.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import com.example.flexioffice.R
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.time.LocalDate

/** Data class for home office bookings in Firestore */
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
    // Empty constructor for Firestore
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

@StringRes
fun BookingStatus.labelRes() =
    when (this) {
        BookingStatus.APPROVED -> R.string.booking_item_status_approved
        BookingStatus.PENDING -> R.string.booking_item_status_pending
        BookingStatus.DECLINED -> R.string.booking_item_status_declined
        BookingStatus.CANCELLED -> R.string.booking_item_status_cancelled
    }

fun BookingStatus.statusColor() =
    when (this) {
        BookingStatus.APPROVED -> R.color.status_approved
        BookingStatus.PENDING -> R.color.status_pending
        BookingStatus.DECLINED -> R.color.status_declined
        BookingStatus.CANCELLED -> R.color.status_cancelled
    }

fun BookingStatus.statusIcon() =
    when (this) {
        BookingStatus.APPROVED -> Icons.Default.CheckCircle
        BookingStatus.PENDING -> Icons.Default.Info
        BookingStatus.DECLINED -> Icons.Default.Close
        BookingStatus.CANCELLED -> Icons.Default.Close
    }
