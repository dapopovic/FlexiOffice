package com.example.flexioffice.data

import android.util.Log
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class NotificationRepository
@Inject
constructor(
        private val firestore: FirebaseFirestore,
) {
    companion object {
        private const val TAG = "NotificationRepository"
        private const val NOTIFICATIONS_COLLECTION = "notifications"
    }

    /** Sendet eine Notification über Booking-Status-Änderung */
    suspend fun sendBookingStatusNotification(
            booking: Booking,
            newStatus: BookingStatus,
            reviewerName: String,
    ): Result<Unit> {
        return try {
            // Hole das FCM Token des Antragstellers
            val userToken = getUserFCMToken(booking.userId)

            if (userToken.isNullOrEmpty()) {
                Log.w(TAG, "Kein FCM Token für User ${booking.userId} gefunden")
                return Result.success(Unit) // Nicht kritisch
            }

            // Erstelle Notification-Datenstruktur
            val notificationData =
                    createBookingStatusNotificationData(
                            booking = booking,
                            newStatus = newStatus,
                            reviewerName = reviewerName,
                            fcmToken = userToken,
                    )

            // Speichere Notification in Firestore für Cloud Function
            saveNotificationForCloudFunction(notificationData)

            Log.d(TAG, "Booking Status Notification erstellt für User ${booking.userId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Senden der Booking Status Notification", e)
            Result.failure(e)
        }
    }

    /** Sendet eine Notification über neue Booking-Anfrage an Manager */
    suspend fun sendNewBookingRequestNotification(
            booking: Booking,
            managerUserId: String,
    ): Result<Unit> {
        return try {
            // Hole das FCM Token des Managers
            val managerToken = getUserFCMToken(managerUserId)

            if (managerToken.isNullOrEmpty()) {
                Log.w(TAG, "Kein FCM Token für Manager $managerUserId gefunden")
                return Result.success(Unit) // Nicht kritisch
            }

            // Erstelle Notification-Datenstruktur
            val notificationData =
                    createNewRequestNotificationData(
                            booking = booking,
                            fcmToken = managerToken,
                    )

            // Speichere Notification in Firestore für Cloud Function
            saveNotificationForCloudFunction(notificationData)

            Log.d(TAG, "New Booking Request Notification erstellt für Manager $managerUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Senden der New Request Notification", e)
            Result.failure(e)
        }
    }

    private suspend fun getUserFCMToken(userId: String): String? =
            try {
                val userDoc =
                        firestore.collection(User.COLLECTION_NAME).document(userId).get().await()

                userDoc.getString(User.FCM_TOKEN_FIELD)
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Abrufen des FCM Tokens für User $userId", e)
                null
            }

    private fun createBookingStatusNotificationData(
            booking: Booking,
            newStatus: BookingStatus,
            reviewerName: String,
            fcmToken: String,
    ): Map<String, Any> {
        val title =
                when (newStatus) {
                    BookingStatus.APPROVED -> "Home-Office genehmigt! ✅"
                    BookingStatus.DECLINED -> "Home-Office abgelehnt ❌"
                    else -> "Buchungsstatus geändert"
                }

        val body =
                when (newStatus) {
                    BookingStatus.APPROVED ->
                            "Ihr Home-Office-Antrag für ${booking.date} wurde von $reviewerName genehmigt."
                    BookingStatus.DECLINED ->
                            "Ihr Home-Office-Antrag für ${booking.date} wurde von $reviewerName abgelehnt."
                    else -> "Der Status Ihrer Buchung wurde geändert."
                }

        return mapOf(
                "type" to "booking_status_update",
                "fcmToken" to fcmToken,
                "title" to title,
                "body" to body,
                "data" to
                        mapOf(
                                "type" to "booking_status_update",
                                "bookingId" to booking.id,
                                "status" to newStatus.name,
                                "userName" to booking.userName,
                                "date" to booking.dateString,
                                "reviewerName" to reviewerName,
                        ),
                "createdAt" to System.currentTimeMillis(),
                "processed" to false,
        )
    }

    private fun createNewRequestNotificationData(
            booking: Booking,
            fcmToken: String,
    ): Map<String, Any> =
            mapOf(
                    "type" to "new_booking_request",
                    "fcmToken" to fcmToken,
                    "title" to "Neue Buchungsanfrage 📋",
                    "body" to "${booking.userName} möchte Home-Office am ${booking.date}",
                    "data" to
                            mapOf(
                                    "type" to "new_booking_request",
                                    "bookingId" to booking.id,
                                    "userName" to booking.userName,
                                    "date" to booking.dateString,
                                    "comment" to booking.comment,
                            ),
                    "createdAt" to System.currentTimeMillis(),
                    "processed" to false,
            )

    private suspend fun saveNotificationForCloudFunction(notificationData: Map<String, Any>) {
        try {
            firestore.collection(NOTIFICATIONS_COLLECTION).add(notificationData).await()

            Log.d(TAG, "Notification für Cloud Function gespeichert")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Speichern der Notification", e)
            throw e
        }
    }
}
