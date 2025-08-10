package com.example.flexioffice.data

import android.util.Log
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.TeamInvitation
import com.example.flexioffice.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

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

        // Simple in-memory token cache to minimize reads
        private val tokenCache = mutableMapOf<String, String>()

        /** Sends a notification to a user that they were invited to a team */
        suspend fun sendTeamInvitationNotification(invitation: TeamInvitation): Result<Unit> {
            return try {
                val inviteeToken = getUserFCMToken(invitation.invitedUserId)
                if (inviteeToken.isNullOrEmpty()) {
                    Log.w(TAG, "No FCM Token found for invited user ${invitation.invitedUserId}")
                    return Result.success(Unit)
                }

                val data =
                    mapOf(
                        "type" to "team_invitation",
                        "fcmToken" to inviteeToken,
                        "title" to "Team-Einladung",
                        "body" to
                            "${invitation.invitedByUserName} hat Sie in das Team \"${invitation.teamName}\" eingeladen.",
                        "data" to
                            mapOf(
                                "type" to "team_invitation",
                                "invitationId" to invitation.id,
                                "teamId" to invitation.teamId,
                                "teamName" to invitation.teamName,
                                "invitedByUserId" to invitation.invitedByUserId,
                                "invitedByUserName" to invitation.invitedByUserName,
                            ),
                        "createdAt" to System.currentTimeMillis(),
                        "processed" to false,
                    )

                saveNotificationForCloudFunction(data)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending team invitation notification", e)
                Result.failure(e)
            }
        }

        /** Sends a notification to the manager when a user responds to an invitation */
        suspend fun sendTeamInvitationResponseNotification(invitation: TeamInvitation): Result<Unit> {
            return try {
                val managerToken = getUserFCMToken(invitation.invitedByUserId)
                if (managerToken.isNullOrEmpty()) {
                    Log.w(TAG, "No FCM Token found for manager ${invitation.invitedByUserId}")
                    return Result.success(Unit)
                }

                val accepted = invitation.status.equals(TeamInvitation.STATUS_ACCEPTED, ignoreCase = true)
                val title = if (accepted) "Einladung akzeptiert" else "Einladung abgelehnt"
                // Keine PII (E-Mail) – stattdessen Display Name oder generischer Platzhalter
                val actorName = invitation.invitedUserDisplayName.ifBlank { "Ein Nutzer" }
                val body =
                    if (accepted) {
                        "$actorName ist dem Team \"${invitation.teamName}\" beigetreten."
                    } else {
                        "$actorName hat die Einladung für \"${invitation.teamName}\" abgelehnt."
                    }

                val data =
                    mapOf(
                        "type" to "team_invitation_response",
                        "fcmToken" to managerToken,
                        "title" to title,
                        "body" to body,
                        "data" to
                            mapOf(
                                "type" to "team_invitation_response",
                                "invitationId" to invitation.id,
                                "teamId" to invitation.teamId,
                                "teamName" to invitation.teamName,
                                "status" to invitation.status,
                                "invitedUserId" to invitation.invitedUserId,
                                "invitedUserDisplayName" to invitation.invitedUserDisplayName,
                            ),
                        "createdAt" to System.currentTimeMillis(),
                        "processed" to false,
                    )

                saveNotificationForCloudFunction(data)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending team invitation response notification", e)
                Result.failure(e)
            }
        }

        /** Sends a notification that an invitation was cancelled by the manager */
        suspend fun sendTeamInvitationCancelledNotification(invitation: TeamInvitation): Result<Unit> {
            return try {
                val inviteeToken = getUserFCMToken(invitation.invitedUserId)
                if (inviteeToken.isNullOrEmpty()) {
                    Log.w(TAG, "No FCM Token found for invited user ${invitation.invitedUserId}")
                    return Result.success(Unit)
                }

                val data =
                    mapOf(
                        "type" to "team_invitation_cancelled",
                        "fcmToken" to inviteeToken,
                        "title" to "Einladung storniert",
                        "body" to "Die Einladung für das Team \"${invitation.teamName}\" wurde storniert.",
                        "data" to
                            mapOf(
                                "type" to "team_invitation_cancelled",
                                "invitationId" to invitation.id,
                                "teamId" to invitation.teamId,
                                "teamName" to invitation.teamName,
                            ),
                        "createdAt" to System.currentTimeMillis(),
                        "processed" to false,
                    )

                saveNotificationForCloudFunction(data)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending team invitation cancelled notification", e)
                Result.failure(e)
            }
        }

        /** Sends a notification about a booking status change */
        suspend fun sendBookingStatusNotification(
            booking: Booking,
            newStatus: BookingStatus,
            reviewerName: String,
        ): Result<Unit> {
            return try {
                // Get the FCM token of the requester
                val userToken = getUserFCMToken(booking.userId)

                if (userToken.isNullOrEmpty()) {
                    Log.w(TAG, "No FCM Token found for User ${booking.userId}")
                    return Result.success(Unit) // Not critical
                }

                // Create notification data structure
                val notificationData =
                    createBookingStatusNotificationData(
                        booking = booking,
                        newStatus = newStatus,
                        reviewerName = reviewerName,
                        fcmToken = userToken,
                    )

                // Save notification in Firestore for Cloud Function
                saveNotificationForCloudFunction(notificationData)

                Log.d(TAG, "Booking Status Notification created for User ${booking.userId}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending booking status notification", e)
                Result.failure(e)
            }
        }

        /** Sends a notification about a new booking request to the manager */
        suspend fun sendNewBookingRequestNotification(
            booking: Booking,
            managerUserId: String,
        ): Result<Unit> {
            return try {
                // Get the FCM token of the manager
                val managerToken = getUserFCMToken(managerUserId)

                if (managerToken.isNullOrEmpty()) {
                    Log.w(TAG, "No FCM Token found for Manager $managerUserId")
                    return Result.success(Unit) // Not critical
                }

                // Create notification data structure
                val notificationData =
                    createNewRequestNotificationData(
                        booking = booking,
                        fcmToken = managerToken,
                    )

                // Save notification in Firestore for Cloud Function
                saveNotificationForCloudFunction(notificationData)

                Log.d(TAG, "New Booking Request Notification created for Manager $managerUserId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending new request notification", e)
                Result.failure(e)
            }
        }

        /** Retrieves the FCM token for a user */
        private suspend fun getUserFCMToken(userId: String): String? =
            try {
                tokenCache[userId]?.let { return it }

                val userDoc =
                    firestore
                        .collection(User.COLLECTION_NAME)
                        .document(userId)
                        .get()
                        .await()

                val token = userDoc.getString(User.FCM_TOKEN_FIELD)
                if (!token.isNullOrEmpty()) tokenCache[userId] = token
                token
            } catch (e: Exception) {
                Log.e(TAG, "Error retrieving FCM token for User $userId", e)
                null
            }

        /** Creates the notification data for a booking status change */
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

        /** Creates the notification data for a new booking request */
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

        /** Saves a notification for Cloud Function processing */
        private suspend fun saveNotificationForCloudFunction(notificationData: Map<String, Any>) {
            try {
                firestore.collection(NOTIFICATIONS_COLLECTION).add(notificationData).await()

                Log.d(TAG, "Notification saved for Cloud Function")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification", e)
                throw e
            }
        }
    }
