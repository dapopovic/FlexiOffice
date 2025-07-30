package com.example.flexioffice.data

import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.BookingType
import com.example.flexioffice.data.model.Team
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Repository für Buchungs-Datenoperationen in Firestore */
@Singleton
class BookingRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
    ) {
        /** Lädt alle Buchungen für einen bestimmten Monat */
        suspend fun getBookingsForMonth(month: YearMonth): List<Booking> {
            val startDate = month.atDay(1)
            val endDate = month.atEndOfMonth()
            val startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

            return firestore
                .collection(Booking.COLLECTION_NAME)
                .whereGreaterThanOrEqualTo(Booking.DATE_FIELD, startDateStr)
                .whereLessThanOrEqualTo(Booking.DATE_FIELD, endDateStr)
                .get()
                .await()
                .toObjects(Booking::class.java)
        }

        /** Erstellt eine neue Buchung aus einem existierenden Booking-Objekt */
        suspend fun createBooking(booking: Booking): Result<String> =
            try {
                val docRef = firestore.collection(Booking.COLLECTION_NAME).document()
                val bookingWithId = booking.copy(id = docRef.id)
                docRef.set(bookingWithId).await()
                Result.success(docRef.id)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Erstellt eine neue Buchung aus Datum und Kommentar */
        suspend fun createBooking(
            date: LocalDate,
            comment: String,
            userId: String,
            userName: String,
            teamId: String,
            type: BookingType = BookingType.HOME_OFFICE,
        ): Result<String> {
            return try {
                // Validiere das Datum
                if (date.isBefore(LocalDate.now())) {
                    return Result.failure(IllegalArgumentException("Buchungen für vergangene Tage sind nicht möglich"))
                }

                // Prüfe auf existierende Buchungen
                val existingBooking =
                    getUserBookingsForDate(userId, date).getOrNull()?.firstOrNull {
                        it.status != BookingStatus.CANCELLED
                    }
                if (existingBooking != null) {
                    return Result.failure(
                        IllegalArgumentException("Sie haben bereits eine aktive Buchung für diesen Tag"),
                    )
                }

                // Hole Team-Informationen für den Reviewer
                val team =
                    try {
                        val teamDoc =
                            firestore
                                .collection(Team.COLLECTION_NAME)
                                .document(teamId)
                                .get()
                                .await()
                        teamDoc.toObject(Team::class.java)
                            ?: return Result.failure(IllegalArgumentException("Team nicht gefunden"))
                    } catch (e: Exception) {
                        return Result.failure(IllegalArgumentException("Fehler beim Laden des Teams"))
                    }

                // Prüfe ob das Team einen Manager hat
                if (team.managerId.isNullOrEmpty()) {
                    return Result.failure(IllegalArgumentException("Das Team hat keinen Manager zugewiesen"))
                }

                val booking =
                    Booking(
                        dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        userId = userId,
                        userName = userName,
                        teamId = teamId,
                        type = type,
                        comment = comment,
                        status = BookingStatus.PENDING,
                        createdAt = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        reviewerId = team.managerId,
                    )
                createBooking(booking)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /** Lädt Buchungen eines Benutzers für ein bestimmtes Datum */
        private suspend fun getUserBookingsForDate(
            userId: String,
            date: LocalDate,
        ): Result<List<Booking>> =
            try {
                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val querySnapshot =
                    firestore
                        .collection(Booking.COLLECTION_NAME)
                        .whereEqualTo(Booking.USER_ID_FIELD, userId)
                        .whereEqualTo(Booking.DATE_FIELD, dateStr)
                        .get()
                        .await()

                Result.success(querySnapshot.toObjects(Booking::class.java))
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Lädt alle Buchungen für ein Team in einem bestimmten Zeitraum */
        suspend fun getTeamBookingsInRange(
            teamId: String,
            startDate: LocalDate,
            endDate: LocalDate,
        ): Result<List<Booking>> =
            try {
                val startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val endDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                val querySnapshot =
                    firestore
                        .collection(Booking.COLLECTION_NAME)
                        .whereEqualTo(Booking.TEAM_ID_FIELD, teamId)
                        .whereGreaterThanOrEqualTo(Booking.DATE_FIELD, startDateStr)
                        .whereLessThanOrEqualTo(Booking.DATE_FIELD, endDateStr)
                        .get()
                        .await()

                val bookings = querySnapshot.toObjects(Booking::class.java)
                Result.success(bookings)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Stream für Team-Buchungen eines bestimmten Monats */
        fun getTeamBookingsStream(
            teamId: String,
            year: Int,
            month: Int,
        ): Flow<Result<List<Booking>>> =
            callbackFlow {
                val startDate = LocalDate.of(year, month, 1)
                val endDate = startDate.withDayOfMonth(startDate.lengthOfMonth())
                val startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val endDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                val listenerRegistration =
                    firestore
                        .collection(Booking.COLLECTION_NAME)
                        .whereEqualTo(Booking.TEAM_ID_FIELD, teamId)
                        .whereGreaterThanOrEqualTo(Booking.DATE_FIELD, startDateStr)
                        .whereLessThanOrEqualTo(Booking.DATE_FIELD, endDateStr)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(Result.failure(error))
                                return@addSnapshotListener
                            }
                            if (snapshot != null) {
                                val bookings = snapshot.toObjects(Booking::class.java)
                                trySend(Result.success(bookings))
                            }
                        }
                awaitClose { listenerRegistration.remove() }
            }

        /** Lädt Buchungen für einen Benutzer */
        suspend fun getUserBookings(userId: String): Result<List<Booking>> =
            try {
                val querySnapshot =
                    firestore
                        .collection(Booking.COLLECTION_NAME)
                        .whereEqualTo(Booking.USER_ID_FIELD, userId)
                        .orderBy(Booking.DATE_FIELD)
                        .get()
                        .await()

                val bookings = querySnapshot.toObjects(Booking::class.java)
                Result.success(bookings)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Stream für Buchungen eines Benutzers */
        fun getUserBookingsStream(userId: String): Flow<Result<List<Booking>>> =
            callbackFlow {
                val listenerRegistration =
                    firestore
                        .collection(Booking.COLLECTION_NAME)
                        .whereEqualTo(Booking.USER_ID_FIELD, userId)
                        .orderBy(Booking.DATE_FIELD)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(Result.failure(error))
                                return@addSnapshotListener
                            }
                            if (snapshot != null) {
                                val bookings = snapshot.toObjects(Booking::class.java)
                                trySend(Result.success(bookings))
                            }
                        }
                awaitClose { listenerRegistration.remove() }
            }

        /** Aktualisiert den Status einer Buchung */
        suspend fun updateBookingStatus(
            bookingId: String,
            status: BookingStatus,
            reviewerId: String,
        ): Result<Unit> =
            try {
                firestore
                    .collection(Booking.COLLECTION_NAME)
                    .document(bookingId)
                    .update(
                        mapOf(
                            Booking.STATUS_FIELD to status,
                            Booking.REVIEWER_ID_FIELD to reviewerId,
                        ),
                    ).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Erstellt Demo-Buchungen für Testing */
        suspend fun createDemoBookings(teamId: String): Result<Unit> =
            try {
                val currentDate = LocalDate.now()
                val demoBookings =
                    listOf(
                        Booking(
                            userId = "demo_user_1",
                            userName = "Max Mustermann",
                            teamId = teamId,
                            dateString =
                                currentDate.format(
                                    DateTimeFormatter.ISO_LOCAL_DATE,
                                ),
                            type = BookingType.HOME_OFFICE,
                            status = BookingStatus.APPROVED,
                            comment = "Home Office Tag",
                            createdAt = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        ),
                        Booking(
                            userId = "demo_user_2",
                            userName = "Anna Schmidt",
                            teamId = teamId,
                            dateString =
                                currentDate
                                    .plusDays(1)
                                    .format(DateTimeFormatter.ISO_LOCAL_DATE),
                            type = BookingType.HOME_OFFICE,
                            status = BookingStatus.APPROVED,
                            comment = "Remote Work",
                            createdAt = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        ),
                        Booking(
                            userId = "demo_user_3",
                            userName = "Tom Weber",
                            teamId = teamId,
                            dateString =
                                currentDate
                                    .plusDays(2)
                                    .format(DateTimeFormatter.ISO_LOCAL_DATE),
                            type = BookingType.HOME_OFFICE,
                            status = BookingStatus.APPROVED,
                            comment = "Home Office",
                            createdAt = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        ),
                    )

                demoBookings.forEach { booking -> createBooking(booking) }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
    }
