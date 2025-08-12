package com.example.flexioffice.data

import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.BookingType
import com.example.flexioffice.data.model.Team
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private const val FIRESTORE_BATCH_LIMIT = 500

/** Repository für Buchungs-Datenoperationen in Firestore */
@Singleton
class BookingRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
    ) {
        /** Loads all bookings for a specific month */
        suspend fun getBookingsForMonth(month: YearMonth): List<Booking> {
            val startDate = month.atDay(1)
            val endDate = month.atEndOfMonth()
            val startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

            return firestore
                .collection(Booking.COLLECTION_NAME)
                .whereGreaterThanOrEqualTo(Booking.DATE_FIELD, startDateStr)
                .whereLessThanOrEqualTo(Booking.DATE_FIELD, endDateStr)
                .orderBy(Booking.DATE_FIELD, Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(Booking::class.java)
        }

        /** Creates a new booking from an existing Booking object */
        suspend fun createBooking(booking: Booking): Result<String> =
            try {
                val docRef = firestore.collection(Booking.COLLECTION_NAME).document()
                val bookingWithId = booking.copy(id = docRef.id)
                docRef.set(bookingWithId).await()
                Result.success(docRef.id)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Creates a new booking from date and comment */
        suspend fun createBooking(
            date: LocalDate,
            comment: String,
            userId: String,
            userName: String,
            teamId: String,
            type: BookingType = BookingType.HOME_OFFICE,
        ): Result<String> {
            return try {
                // validate date
                if (date.isBefore(LocalDate.now())) {
                    return Result.failure(
                        IllegalArgumentException("Buchungen für vergangene Tage sind nicht möglich"),
                    )
                }

                // check if user already has a booking for this date
                val existingBooking =
                    getUserBookingsForDate(userId, date).getOrNull()?.firstOrNull {
                        it.status != BookingStatus.CANCELLED
                    }
                if (existingBooking != null) {
                    return Result.failure(
                        IllegalArgumentException(
                            "Sie haben bereits eine aktive Buchung für diesen Tag",
                        ),
                    )
                }

                // Load the team to check if it has a manager
                val team =
                    try {
                        val teamDoc =
                            firestore
                                .collection(Team.COLLECTION_NAME)
                                .document(teamId)
                                .get()
                                .await()
                        teamDoc.toObject(Team::class.java)
                            ?: return Result.failure(
                                IllegalArgumentException("Team nicht gefunden"),
                            )
                    } catch (e: Exception) {
                        return Result.failure(
                            IllegalArgumentException(
                                "Fehler beim Laden des Teams: ${e.message}",
                                e,
                            ),
                        )
                    }

                // Check if the team has a manager
                if (team.managerId.isEmpty()) {
                    return Result.failure(
                        IllegalArgumentException("Das Team hat keinen Manager zugewiesen"),
                    )
                }

                val statusForRequester =
                    if (team.managerId == userId) {
                        BookingStatus.APPROVED
                    } else {
                        BookingStatus.PENDING
                    }

                val booking =
                    Booking(
                        dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        userId = userId,
                        userName = userName,
                        teamId = teamId,
                        type = type,
                        comment = comment,
                        status = statusForRequester,
                        createdAt = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        reviewerId = team.managerId,
                    )
                createBooking(booking)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        suspend fun getBookingById(id: String): Result<Booking> =
            try {
                val docSnapshot =
                    firestore
                        .collection(Booking.COLLECTION_NAME)
                        .document(id)
                        .get()
                        .await()

                if (docSnapshot.exists()) {
                    Result.success(docSnapshot.toObject(Booking::class.java) ?: Booking())
                } else {
                    Result.failure(NoSuchElementException("Booking with ID $id not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Loads user bookings for a specific date */
        suspend fun getUserBookingsForDate(
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

        /** Loads all bookings for a team in a specific time range */
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
                        .orderBy(Booking.DATE_FIELD, Query.Direction.ASCENDING)
                        .get()
                        .await()

                val bookings = querySnapshot.toObjects(Booking::class.java)
                Result.success(bookings)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Gets a stream of team bookings for a specific month */
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
                        .orderBy(Booking.DATE_FIELD, Query.Direction.ASCENDING)
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

        /** Gets a stream of team pending booking requests (PENDING Status) */
        fun getTeamPendingRequestsStream(teamId: String): Flow<Result<List<Booking>>> =
            callbackFlow {
                val listenerRegistration =
                    firestore
                        .collection(Booking.COLLECTION_NAME)
                        .whereEqualTo(Booking.TEAM_ID_FIELD, teamId)
                        .whereEqualTo(Booking.STATUS_FIELD, BookingStatus.PENDING)
                        .orderBy(Booking.DATE_FIELD, Query.Direction.ASCENDING)
                        .limit(200)
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

        /** Loads bookings for a user */
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

        /** Gets a stream of user bookings */
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

        /** Updates the status of a booking */
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

        /**
         * Updates the status of multiple bookings in a batch
         * This method is useful for updating multiple bookings at once,
         *  e.g. when a manager wants to approve or reject multiple requests simultaneously
         *
         * @param bookingIds List of booking IDs to update
         * @param status The new status to apply to all bookings
         * @param reviewerId The ID of the user who reviewed the bookings
         * @return Result<Unit> Successful execution or error
         * @throws IllegalArgumentException If the batch size exceeds Firestore limit
         * @throws Exception For other errors during Firestore operations
         */
        suspend fun updateBookingStatusBatch(
            bookingIds: List<String>,
            status: BookingStatus,
            reviewerId: String,
        ): Result<Unit> {
            return try {
                if (bookingIds.isEmpty()) {
                    return Result.success(Unit)
                }

                require(bookingIds.size <= FIRESTORE_BATCH_LIMIT) {
                    "Batch size exceeds Firestore limit of $FIRESTORE_BATCH_LIMIT"
                }

                val batch = firestore.batch()
                val updates =
                    mapOf(
                        Booking.STATUS_FIELD to status,
                        Booking.REVIEWER_ID_FIELD to reviewerId,
                    )

                bookingIds.forEach { bookingId ->
                    val docRef = firestore.collection(Booking.COLLECTION_NAME).document(bookingId)
                    batch.update(docRef, updates)
                }

                batch.commit().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
