package com.example.flexioffice.data

import android.util.Log
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.BookingType
import com.example.flexioffice.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
    ) {
        suspend fun getUserBookings(userId: String): List<Booking> {
            val snapshot =
                firestore
                    .collection(Booking.COLLECTION_NAME)
                    .whereEqualTo(Booking.USER_ID_FIELD, userId)
                    .get()
                    .await()

            return snapshot.documents
                .mapNotNull { doc ->
                    try {
                        val localDateMap = doc.get("localDate") as? Map<*, *>
                        val localDate =
                            if (localDateMap != null) {
                                LocalDate.of(
                                    (localDateMap["year"] as Long).toInt(),
                                    (localDateMap["monthValue"] as Long).toInt(),
                                    (localDateMap["dayOfMonth"] as Long).toInt(),
                                )
                            } else {
                                LocalDate.parse(doc.getString("date"))
                            }
                        Log.d("BookingRepository", "Lade Buchung: ${doc.id} für Datum: $localDate")
                        Booking(
                            id = doc.id,
                            userId = doc.getString(Booking.USER_ID_FIELD) ?: "",
                            userName = doc.getString(Booking.USER_NAME_FIELD) ?: "",
                            teamId = doc.getString(Booking.TEAM_ID_FIELD) ?: "",
                            reviewerId = doc.getString(Booking.REVIEWER_ID_FIELD) ?: "",
                            dateString =
                                doc.getString(Booking.DATE_FIELD)
                                    ?: localDate.toString(),
                            type =
                                BookingType.valueOf(
                                    doc.getString(Booking.TYPE_FIELD)
                                        ?: BookingType.HOME_OFFICE.name,
                                ),
                            status =
                                BookingStatus.valueOf(
                                    doc.getString(Booking.STATUS_FIELD)
                                        ?: BookingStatus.PENDING.name,
                                ),
                            comment = doc.getString(Booking.COMMENT_FIELD) ?: "",
                            createdAt = doc.getString(Booking.CREATED_AT_FIELD) ?: "",
                        )
                    } catch (e: Exception) {
                        Log.e(
                            "BookingRepository",
                            "Fehler beim Parsen der Buchung: ${doc.id}",
                            e,
                        )
                        null
                    }
                }.sortedByDescending { it.date }
        }

        suspend fun updateBookingStatus(
            bookingId: String,
            newStatus: BookingStatus,
        ) {
            try {
                firestore
                    .collection(Booking.COLLECTION_NAME)
                    .document(bookingId)
                    .update(Booking.STATUS_FIELD, newStatus.name)
                    .await()
            } catch (e: Exception) {
                Log.e(
                    "BookingRepository",
                    "Fehler beim Aktualisieren des Buchungsstatus",
                    e,
                )
                throw e
            }
        }

        suspend fun createBooking(booking: Booking) {
            Log.d("BookingRepository", "Erstelle neue Buchung für Datum: ${booking.date}")
            val bookingMap =
                mapOf(
                    Booking.ID_FIELD to booking.id,
                    Booking.USER_ID_FIELD to booking.userId,
                    Booking.USER_NAME_FIELD to booking.userName,
                    Booking.TEAM_ID_FIELD to booking.teamId,
                    Booking.REVIEWER_ID_FIELD to booking.reviewerId,
                    Booking.DATE_FIELD to booking.dateString,
                    Booking.COMMENT_FIELD to booking.comment,
                    Booking.TYPE_FIELD to booking.type.name,
                    Booking.STATUS_FIELD to booking.status.name,
                    Booking.CREATED_AT_FIELD to booking.createdAt,
                )

            try {
                Log.d("BookingRepository", "Sende Daten an Firestore...")
                val documentRef = firestore.collection(Booking.COLLECTION_NAME).document()
                val bookingWithId = bookingMap + (Booking.ID_FIELD to documentRef.id)

                documentRef.set(bookingWithId).await()
                Log.d(
                    "BookingRepository",
                    "Buchung erfolgreich erstellt mit ID: ${documentRef.id}",
                )
            } catch (e: Exception) {
                Log.e("BookingRepository", "Fehler beim Erstellen der Buchung", e)
                throw e
            }
        }
    }
