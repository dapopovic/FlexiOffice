package com.example.flexioffice.data

import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.BookingType
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
                    .collection("bookings")
                    .whereEqualTo("userId", userId)
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

                        Booking(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            teamId = doc.getString("teamId") ?: "",
                            reviewerId = doc.getString("reviewerId") ?: "",
                            dateString = doc.getString("date") ?: localDate.toString(),
                            type = BookingType.valueOf(doc.getString("type") ?: BookingType.HOME_OFFICE.name),
                            status = BookingStatus.valueOf(doc.getString("status") ?: BookingStatus.PENDING.name),
                            comment = doc.getString("comment") ?: "",
                            createdAt = doc.getString("createdAt") ?: "",
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.date }
        }

        suspend fun createBooking(booking: Booking) {
            android.util.Log.d("BookingRepository", "Erstelle neue Buchung für Datum: ${booking.date}")
            val bookingMap =
                mapOf(
                    "id" to booking.id,
                    "userId" to booking.userId,
                    "userName" to booking.userName,
                    "teamId" to booking.teamId,
                    "reviewerId" to booking.reviewerId,
                    "date" to booking.dateString,
                    "comment" to booking.comment,
                    "type" to booking.type.name,
                    "status" to booking.status.name,
                    "createdAt" to booking.createdAt,
                )

            try {
                android.util.Log.d("BookingRepository", "Sende Daten an Firestore...")
                val documentRef = firestore.collection("bookings").document()
                val bookingWithId = bookingMap + ("id" to documentRef.id)

                documentRef.set(bookingWithId).await()
                android.util.Log.d("BookingRepository", "Buchung erfolgreich erstellt mit ID: ${documentRef.id}")
            } catch (e: Exception) {
                android.util.Log.e("BookingRepository", "Fehler beim Erstellen der Buchung", e)
                throw e
            }
        }
    }
