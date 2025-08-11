package com.example.flexioffice.data

import app.cash.turbine.test
import com.example.flexioffice.MainDispatcherRule
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.BookingType
import com.example.flexioffice.data.model.Team
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class BookingRepositoryTest {
    @get:Rule
    val mockKRule = MockKRule(this)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var firestore: FirebaseFirestore

    @MockK
    private lateinit var bookingsCollection: CollectionReference

    @MockK
    private lateinit var teamsCollection: CollectionReference

    @MockK
    private lateinit var query: Query

    @MockK
    private lateinit var querySnapshot: QuerySnapshot

    @MockK
    private lateinit var documentReference: DocumentReference

    @MockK
    private lateinit var documentSnapshot: DocumentSnapshot

    @MockK
    private lateinit var writeBatch: WriteBatch

    @MockK
    private lateinit var listenerRegistration: ListenerRegistration

    private lateinit var repository: BookingRepository

    @Before
    fun setup() {
        every { firestore.collection(Booking.COLLECTION_NAME) } returns bookingsCollection
        every { firestore.collection(Team.COLLECTION_NAME) } returns teamsCollection
        repository = BookingRepository(firestore)
    }

    @Test
    fun `getBookingsForMonth returns bookings for given month`() =
        runTest {
            // Given
            val yearMonth = YearMonth.of(2024, 3)
            val startDate = "2024-03-01"
            val endDate = "2024-03-31"
            val expectedBookings =
                listOf(
                    createTestBooking(id = "1", date = "2024-03-15"),
                    createTestBooking(id = "2", date = "2024-03-20"),
                )

            every { bookingsCollection.whereGreaterThanOrEqualTo(Booking.DATE_FIELD, startDate) } returns query
            every { query.whereLessThanOrEqualTo(Booking.DATE_FIELD, endDate) } returns query
            every { query.orderBy(Booking.DATE_FIELD, Query.Direction.ASCENDING) } returns query
            every { query.get() } returns Tasks.forResult(querySnapshot)
            every { querySnapshot.toObjects(Booking::class.java) } returns expectedBookings

            // When
            val result = repository.getBookingsForMonth(yearMonth)

            // Then
            assertEquals(expectedBookings, result)
            verify { bookingsCollection.whereGreaterThanOrEqualTo(Booking.DATE_FIELD, startDate) }
            verify { query.whereLessThanOrEqualTo(Booking.DATE_FIELD, endDate) }
        }

    @Test
    fun `createBooking with Booking object succeeds`() =
        runTest {
            // Given
            val booking = createTestBooking(id = "", date = "2024-03-15")
            val documentId = "generated_id"
            val expectedBookingWithId = booking.copy(id = documentId)

            every { bookingsCollection.document() } returns documentReference
            every { documentReference.id } returns documentId
            every { documentReference.set(expectedBookingWithId) } returns Tasks.forResult(null)

            // When
            val result = repository.createBooking(booking)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(documentId, result.getOrNull())
            verify { documentReference.set(expectedBookingWithId) }
        }

    @Test
    fun `createBooking with Booking object fails on firestore error`() =
        runTest {
            // Given
            val booking = createTestBooking(id = "", date = "2024-03-15")
            val documentId = "generated_id"
            val expectedBookingWithId = booking.copy(id = documentId)
            val exception = RuntimeException("Firestore error")

            every { bookingsCollection.document() } returns documentReference
            every { documentReference.id } returns documentId
            every { documentReference.set(expectedBookingWithId) } returns Tasks.forException(exception)

            // When
            val result = repository.createBooking(booking)

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `createBooking with parameters fails for past date`() =
        runTest {
            // Given
            val pastDate = LocalDate.now().minusDays(1)

            // When
            val result =
                repository.createBooking(
                    date = pastDate,
                    comment = "Test",
                    userId = "user1",
                    userName = "User Name",
                    teamId = "team1",
                )

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertEquals(
                "Buchungen für vergangene Tage sind nicht möglich",
                result.exceptionOrNull()?.message,
            )
        }

    @Test
    fun `createBooking with parameters fails if user already has active booking for date`() =
        runTest {
            // Given
            val futureDate = LocalDate.now().plusDays(1)
            val userId = "user1"
            val existingBooking =
                createTestBooking(
                    id = "existing",
                    userId = userId,
                    status = BookingStatus.APPROVED,
                )

            setupGetUserBookingsForDateMock(userId, futureDate, listOf(existingBooking))

            // When
            val result =
                repository.createBooking(
                    date = futureDate,
                    comment = "Test",
                    userId = userId,
                    userName = "User Name",
                    teamId = "team1",
                )

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertEquals(
                "Sie haben bereits eine aktive Buchung für diesen Tag",
                result.exceptionOrNull()?.message,
            )
        }

    @Test
    fun `createBooking with parameters fails if team not found`() =
        runTest {
            // Given
            val futureDate = LocalDate.now().plusDays(1)
            val userId = "user1"
            val teamId = "team1"

            setupGetUserBookingsForDateMock(userId, futureDate, emptyList())

            every { teamsCollection.document(teamId) } returns documentReference
            every { documentReference.get() } returns Tasks.forResult(documentSnapshot)
            every { documentSnapshot.toObject(Team::class.java) } returns null

            // When
            val result =
                repository.createBooking(
                    date = futureDate,
                    comment = "Test",
                    userId = userId,
                    userName = "User Name",
                    teamId = teamId,
                )

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertEquals("Team nicht gefunden", result.exceptionOrNull()?.message)
        }

    @Test
    fun `createBooking with parameters fails if team has no manager`() =
        runTest {
            // Given
            val futureDate = LocalDate.now().plusDays(1)
            val userId = "user1"
            val teamId = "team1"
            val team = Team(id = teamId, name = "Test Team", managerId = "")

            setupGetUserBookingsForDateMock(userId, futureDate, emptyList())

            every { teamsCollection.document(teamId) } returns documentReference
            every { documentReference.get() } returns Tasks.forResult(documentSnapshot)
            every { documentSnapshot.toObject(Team::class.java) } returns team

            // When
            val result =
                repository.createBooking(
                    date = futureDate,
                    comment = "Test",
                    userId = userId,
                    userName = "User Name",
                    teamId = teamId,
                )

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertEquals("Das Team hat keinen Manager zugewiesen", result.exceptionOrNull()?.message)
        }

    @Test
    fun `createBooking with parameters succeeds with valid data`() =
        runTest {
            // Given
            val futureDate = LocalDate.now().plusDays(1)
            val userId = "user1"
            val userName = "User Name"
            val teamId = "team1"
            val managerId = "manager1"
            val comment = "Test booking"
            val team = Team(id = teamId, name = "Test Team", managerId = managerId)
            val documentId = "generated_id"

            setupGetUserBookingsForDateMock(userId, futureDate, emptyList())

            every { teamsCollection.document(teamId) } returns documentReference
            every { documentReference.get() } returns Tasks.forResult(documentSnapshot)
            every { documentSnapshot.toObject(Team::class.java) } returns team

            every { bookingsCollection.document() } returns documentReference
            every { documentReference.id } returns documentId

            val bookingSlot = slot<Booking>()
            every { documentReference.set(capture(bookingSlot)) } returns Tasks.forResult(null)

            // When
            val result =
                repository.createBooking(
                    date = futureDate,
                    comment = comment,
                    userId = userId,
                    userName = userName,
                    teamId = teamId,
                    type = BookingType.HOME_OFFICE,
                )

            // Then
            assertTrue(result.isSuccess)
            assertEquals(documentId, result.getOrNull())

            val capturedBooking = bookingSlot.captured
            assertEquals(futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE), capturedBooking.dateString)
            assertEquals(userId, capturedBooking.userId)
            assertEquals(userName, capturedBooking.userName)
            assertEquals(teamId, capturedBooking.teamId)
            assertEquals(BookingType.HOME_OFFICE, capturedBooking.type)
            assertEquals(comment, capturedBooking.comment)
            assertEquals(BookingStatus.PENDING, capturedBooking.status)
            assertEquals(managerId, capturedBooking.reviewerId)
        }

    @Test
    fun `getUserBookingsForDate returns bookings successfully`() =
        runTest {
            // Given
            val userId = "user1"
            val date = LocalDate.of(2024, 3, 15)
            val dateStr = "2024-03-15"
            val expectedBookings = listOf(createTestBooking(id = "1", userId = userId, date = dateStr))

            setupGetUserBookingsForDateMock(userId, date, expectedBookings)

            // When
            val result = repository.getUserBookingsForDate(userId, date)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(expectedBookings, result.getOrNull())
        }

    @Test
    fun `getUserBookingsForDate handles firestore error`() =
        runTest {
            // Given
            val userId = "user1"
            val date = LocalDate.of(2024, 3, 15)
            val dateStr = "2024-03-15"
            val exception = RuntimeException("Firestore error")

            every { bookingsCollection.whereEqualTo(Booking.USER_ID_FIELD, userId) } returns query
            every { query.whereEqualTo(Booking.DATE_FIELD, dateStr) } returns query
            every { query.get() } returns Tasks.forException(exception)

            // When
            val result = repository.getUserBookingsForDate(userId, date)

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `getTeamBookingsInRange returns bookings successfully`() =
        runTest {
            // Given
            val teamId = "team1"
            val startDate = LocalDate.of(2024, 3, 1)
            val endDate = LocalDate.of(2024, 3, 31)
            val startDateStr = "2024-03-01"
            val endDateStr = "2024-03-31"
            val expectedBookings =
                listOf(
                    createTestBooking(id = "1", teamId = teamId, date = "2024-03-15"),
                    createTestBooking(id = "2", teamId = teamId, date = "2024-03-20"),
                )

            every { bookingsCollection.whereEqualTo(Booking.TEAM_ID_FIELD, teamId) } returns query
            every { query.whereGreaterThanOrEqualTo(Booking.DATE_FIELD, startDateStr) } returns query
            every { query.whereLessThanOrEqualTo(Booking.DATE_FIELD, endDateStr) } returns query
            every { query.orderBy(Booking.DATE_FIELD, Query.Direction.ASCENDING) } returns query
            every { query.get() } returns Tasks.forResult(querySnapshot)
            every { querySnapshot.toObjects(Booking::class.java) } returns expectedBookings

            // When
            val result = repository.getTeamBookingsInRange(teamId, startDate, endDate)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(expectedBookings, result.getOrNull())
        }

    @Test
    fun `getTeamBookingsInRange handles firestore error`() =
        runTest {
            // Given
            val teamId = "team1"
            val startDate = LocalDate.of(2024, 3, 1)
            val endDate = LocalDate.of(2024, 3, 31)
            val exception = RuntimeException("Firestore error")

            every { bookingsCollection.whereEqualTo(Booking.TEAM_ID_FIELD, teamId) } returns query
            every { query.whereGreaterThanOrEqualTo(any<String>(), any<String>()) } returns query
            every { query.whereLessThanOrEqualTo(any<String>(), any<String>()) } returns query
            every { query.orderBy(any<String>(), Query.Direction.ASCENDING) } returns query
            every { query.get() } returns Tasks.forException(exception)

            // When
            val result = repository.getTeamBookingsInRange(teamId, startDate, endDate)

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `getTeamBookingsStream emits bookings successfully`() =
        runTest {
            // Given
            val teamId = "team1"
            val year = 2024
            val month = 3
            val startDateStr = "2024-03-01"
            val endDateStr = "2024-03-31"
            val expectedBookings =
                listOf(
                    createTestBooking(id = "1", teamId = teamId, date = "2024-03-15"),
                    createTestBooking(id = "2", teamId = teamId, date = "2024-03-20"),
                )

            val listenerSlot = slot<EventListener<QuerySnapshot>>()

            every { bookingsCollection.whereEqualTo(Booking.TEAM_ID_FIELD, teamId) } returns query
            every { query.whereGreaterThanOrEqualTo(Booking.DATE_FIELD, startDateStr) } returns query
            every { query.whereLessThanOrEqualTo(Booking.DATE_FIELD, endDateStr) } returns query
            every { query.orderBy(Booking.DATE_FIELD, Query.Direction.ASCENDING) } returns query
            every { query.addSnapshotListener(capture(listenerSlot)) } returns listenerRegistration
            every { querySnapshot.toObjects(Booking::class.java) } returns expectedBookings

            // When & Then
            repository.getTeamBookingsStream(teamId, year, month).test {
                // Simulate successful snapshot
                listenerSlot.captured.onEvent(querySnapshot, null)

                val emission = awaitItem()
                assertTrue(emission.isSuccess)
                assertEquals(expectedBookings, emission.getOrNull())

                cancelAndIgnoreRemainingEvents()
            }

            verify { listenerRegistration.remove() }
        }

    @Test
    fun `getTeamBookingsStream emits error on firestore error`() =
        runTest {
            // Given
            val teamId = "team1"
            val year = 2024
            val month = 3
            val exception = mockk<FirebaseFirestoreException>()

            val listenerSlot = slot<EventListener<QuerySnapshot>>()

            every { bookingsCollection.whereEqualTo(Booking.TEAM_ID_FIELD, teamId) } returns query
            every { query.whereGreaterThanOrEqualTo(any<String>(), any<String>()) } returns query
            every { query.whereLessThanOrEqualTo(any<String>(), any<String>()) } returns query
            every { query.orderBy(any<String>(), Query.Direction.ASCENDING) } returns query
            every { query.addSnapshotListener(capture(listenerSlot)) } returns listenerRegistration

            // When & Then
            repository.getTeamBookingsStream(teamId, year, month).test {
                // Simulate error
                listenerSlot.captured.onEvent(null, exception)

                val emission = awaitItem()
                assertTrue(emission.isFailure)
                assertEquals(exception, emission.exceptionOrNull())

                cancelAndIgnoreRemainingEvents()
            }

            verify { listenerRegistration.remove() }
        }

    @Test
    fun `getTeamPendingRequestsStream emits pending bookings successfully`() =
        runTest {
            // Given
            val teamId = "team1"
            val expectedBookings =
                listOf(
                    createTestBooking(id = "1", teamId = teamId, status = BookingStatus.PENDING),
                    createTestBooking(id = "2", teamId = teamId, status = BookingStatus.PENDING),
                )

            val listenerSlot = slot<EventListener<QuerySnapshot>>()

            every { bookingsCollection.whereEqualTo(Booking.TEAM_ID_FIELD, teamId) } returns query
            every { query.whereEqualTo(Booking.STATUS_FIELD, BookingStatus.PENDING) } returns query
            every { query.orderBy(Booking.DATE_FIELD, Query.Direction.ASCENDING) } returns query
            every { query.limit(200) } returns query
            every { query.addSnapshotListener(capture(listenerSlot)) } returns listenerRegistration
            every { querySnapshot.toObjects(Booking::class.java) } returns expectedBookings

            // When & Then
            repository.getTeamPendingRequestsStream(teamId).test {
                // Simulate successful snapshot
                listenerSlot.captured.onEvent(querySnapshot, null)

                val emission = awaitItem()
                assertTrue(emission.isSuccess)
                assertEquals(expectedBookings, emission.getOrNull())

                cancelAndIgnoreRemainingEvents()
            }

            verify { listenerRegistration.remove() }
        }

    @Test
    fun `getUserBookings returns bookings successfully`() =
        runTest {
            // Given
            val userId = "user1"
            val expectedBookings =
                listOf(
                    createTestBooking(id = "1", userId = userId),
                    createTestBooking(id = "2", userId = userId),
                )

            every { bookingsCollection.whereEqualTo(Booking.USER_ID_FIELD, userId) } returns query
            every { query.orderBy(Booking.DATE_FIELD) } returns query
            every { query.get() } returns Tasks.forResult(querySnapshot)
            every { querySnapshot.toObjects(Booking::class.java) } returns expectedBookings

            // When
            val result = repository.getUserBookings(userId)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(expectedBookings, result.getOrNull())
        }

    @Test
    fun `getUserBookingsStream emits bookings successfully`() =
        runTest {
            // Given
            val userId = "user1"
            val expectedBookings =
                listOf(
                    createTestBooking(id = "1", userId = userId),
                    createTestBooking(id = "2", userId = userId),
                )

            val listenerSlot = slot<EventListener<QuerySnapshot>>()

            every { bookingsCollection.whereEqualTo(Booking.USER_ID_FIELD, userId) } returns query
            every { query.orderBy(Booking.DATE_FIELD) } returns query
            every { query.addSnapshotListener(capture(listenerSlot)) } returns listenerRegistration
            every { querySnapshot.toObjects(Booking::class.java) } returns expectedBookings

            // When & Then
            repository.getUserBookingsStream(userId).test {
                // Simulate successful snapshot
                listenerSlot.captured.onEvent(querySnapshot, null)

                val emission = awaitItem()
                assertTrue(emission.isSuccess)
                assertEquals(expectedBookings, emission.getOrNull())

                cancelAndIgnoreRemainingEvents()
            }

            verify { listenerRegistration.remove() }
        }

    @Test
    fun `updateBookingStatus succeeds`() =
        runTest {
            // Given
            val bookingId = "booking1"
            val status = BookingStatus.APPROVED
            val reviewerId = "reviewer1"
            val expectedUpdates =
                mapOf(
                    Booking.STATUS_FIELD to status,
                    Booking.REVIEWER_ID_FIELD to reviewerId,
                )

            every { bookingsCollection.document(bookingId) } returns documentReference
            every { documentReference.update(expectedUpdates) } returns Tasks.forResult(null)

            // When
            val result = repository.updateBookingStatus(bookingId, status, reviewerId)

            // Then
            assertTrue(result.isSuccess)
            verify { documentReference.update(expectedUpdates) }
        }

    @Test
    fun `updateBookingStatus handles firestore error`() =
        runTest {
            // Given
            val bookingId = "booking1"
            val status = BookingStatus.APPROVED
            val reviewerId = "reviewer1"
            val exception = RuntimeException("Firestore error")

            every { bookingsCollection.document(bookingId) } returns documentReference
            every { documentReference.update(any<Map<String, Any>>()) } returns Tasks.forException(exception)

            // When
            val result = repository.updateBookingStatus(bookingId, status, reviewerId)

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `updateBookingStatusBatch succeeds with valid booking IDs`() =
        runTest {
            // Given
            val bookingIds = listOf("booking1", "booking2", "booking3")
            val status = BookingStatus.APPROVED
            val reviewerId = "reviewer1"
            val expectedUpdates =
                mapOf(
                    Booking.STATUS_FIELD to status,
                    Booking.REVIEWER_ID_FIELD to reviewerId,
                )

            every { firestore.batch() } returns writeBatch
            bookingIds.forEach { bookingId ->
                every { bookingsCollection.document(bookingId) } returns documentReference
                every { writeBatch.update(documentReference, expectedUpdates) } returns writeBatch
            }
            every { writeBatch.commit() } returns Tasks.forResult(null)

            // When
            val result = repository.updateBookingStatusBatch(bookingIds, status, reviewerId)

            // Then
            assertTrue(result.isSuccess)
            verify { writeBatch.commit() }
            bookingIds.forEach { bookingId ->
                verify { writeBatch.update(any<DocumentReference>(), expectedUpdates) }
            }
        }

    @Test
    fun `updateBookingStatusBatch succeeds with empty list`() =
        runTest {
            // Given
            val bookingIds = emptyList<String>()
            val status = BookingStatus.APPROVED
            val reviewerId = "reviewer1"

            // When
            val result = repository.updateBookingStatusBatch(bookingIds, status, reviewerId)

            // Then
            assertTrue(result.isSuccess)
            // No interactions with firestore should occur
            verify(exactly = 0) { firestore.batch() }
        }

    @Test
    fun `updateBookingStatusBatch fails when batch size exceeds limit`() =
        runTest {
            // Given
            val bookingIds = (1..501).map { "booking$it" } // Exceeds FIRESTORE_BATCH_LIMIT of 500
            val status = BookingStatus.APPROVED
            val reviewerId = "reviewer1"

            // When
            val result = repository.updateBookingStatusBatch(bookingIds, status, reviewerId)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertEquals(
                "Batch size exceeds Firestore limit of 500",
                result.exceptionOrNull()?.message,
            )
        }

    @Test
    fun `updateBookingStatusBatch handles firestore error`() =
        runTest {
            // Given
            val bookingIds = listOf("booking1", "booking2")
            val status = BookingStatus.APPROVED
            val reviewerId = "reviewer1"
            val exception = RuntimeException("Firestore error")

            every { firestore.batch() } returns writeBatch
            bookingIds.forEach { bookingId ->
                every { bookingsCollection.document(bookingId) } returns documentReference
                every { writeBatch.update(documentReference, any<Map<String, Any>>()) } returns writeBatch
            }
            every { writeBatch.commit() } returns Tasks.forException(exception)

            // When
            val result = repository.updateBookingStatusBatch(bookingIds, status, reviewerId)

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    // Helper methods

    private fun createTestBooking(
        id: String = "test_id",
        userId: String = "user1",
        userName: String = "Test User",
        teamId: String = "team1",
        date: String = "2024-03-15",
        type: BookingType = BookingType.HOME_OFFICE,
        status: BookingStatus = BookingStatus.PENDING,
        comment: String = "Test comment",
        createdAt: String = "2024-03-15",
        reviewerId: String = "reviewer1",
    ): Booking =
        Booking(
            id = id,
            userId = userId,
            userName = userName,
            teamId = teamId,
            dateString = date,
            type = type,
            status = status,
            comment = comment,
            createdAt = createdAt,
            reviewerId = reviewerId,
        )

    private fun setupGetUserBookingsForDateMock(
        userId: String,
        date: LocalDate,
        bookings: List<Booking>,
    ) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        every { bookingsCollection.whereEqualTo(Booking.USER_ID_FIELD, userId) } returns query
        every { query.whereEqualTo(Booking.DATE_FIELD, dateStr) } returns query
        every { query.get() } returns Tasks.forResult(querySnapshot)
        every { querySnapshot.toObjects(Booking::class.java) } returns bookings
    }
}
