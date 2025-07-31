package com.example.flexioffice.data

import com.example.flexioffice.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Repository für Benutzer-Datenoperationen in Firestore */
@Singleton
class UserRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val auth: FirebaseAuth,
    ) {
        /** Erstellt einen neuen Benutzer in Firestore */
        suspend fun createUser(
            uid: String,
            user: User,
        ): Result<Unit> =
            try {
                firestore
                    .collection(User.COLLECTION_NAME)
                    .document(uid)
                    .set(user)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Lädt Benutzer-Daten aus Firestore */
        suspend fun getCurrentUser(): User? {
            val uid = auth.currentUser?.uid ?: return null
            return getUser(uid).getOrNull()
        }

        suspend fun getUser(uid: String): Result<User?> =
            try {
                val document =
                    firestore
                        .collection(User.COLLECTION_NAME)
                        .document(uid)
                        .get()
                        .await()

                if (document.exists()) {
                    Result.success(document.toObject(User::class.java)?.copy(id = document.id))
                } else {
                    Result.success(null)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Lädt einen Benutzer anhand seiner ID */
        suspend fun getUserById(uid: String): Result<User?> =
            try {
                val document =
                    firestore
                        .collection(User.COLLECTION_NAME)
                        .document(uid)
                        .get()
                        .await()

                if (document.exists()) {
                    Result.success(document.toObject(User::class.java)?.copy(id = document.id))
                } else {
                    Result.failure(Exception("Benutzer nicht gefunden."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }

        suspend fun getUsersByTeamId(teamId: String): Result<List<User>> =
            try {
                val querySnapshot =
                    firestore
                        .collection(User.COLLECTION_NAME)
                        .whereEqualTo(User.TEAM_ID_FIELD, teamId)
                        .get()
                        .await()

                val users = querySnapshot.toObjects(User::class.java)
                Result.success(users)
            } catch (e: Exception) {
                Result.failure(e)
            }

        fun getUserStream(userId: String): Flow<Result<User>> =
            callbackFlow {
                val listenerRegistration =
                    firestore
                        .collection(User.COLLECTION_NAME)
                        .document(userId)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(Result.failure(error))
                                return@addSnapshotListener
                            }
                            if (snapshot != null && snapshot.exists()) {
                                // Add the document ID to the user object
                                val user = snapshot.toObject(User::class.java)?.copy(id = snapshot.id)
                                if (user != null) {
                                    trySend(Result.success(user))
                                } else {
                                    trySend(Result.failure(Exception("Failed to parse user data.")))
                                }
                            } else {
                                trySend(Result.failure(Exception("User not found.")))
                            }
                        }
                // This is called when the Flow is cancelled/closed
                awaitClose { listenerRegistration.remove() }
            }

        fun getTeamMembersStream(teamId: String): Flow<Result<List<User>>> =
            callbackFlow {
                val listenerRegistration =
                    firestore
                        .collection(User.COLLECTION_NAME)
                        .whereEqualTo(User.TEAM_ID_FIELD, teamId)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(Result.failure(error))
                                return@addSnapshotListener
                            }
                            if (snapshot != null) {
                                val users =
                                    snapshot.documents.mapNotNull { doc ->
                                        doc.toObject(User::class.java)?.copy(id = doc.id)
                                    }
                                trySend(Result.success(users))
                            }
                        }
                awaitClose { listenerRegistration.remove() }
            }

        /** Entfernt ein Mitglied aus einem Team durch Zurücksetzen der TeamId */
        suspend fun removeUserFromTeam(userId: String): Result<Unit> =
            try {
                firestore
                    .collection(User.COLLECTION_NAME)
                    .document(userId)
                    .update(User.TEAM_ID_FIELD, "")
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Aktualisiert Benutzer-Daten in Firestore */
        suspend fun updateUser(
            uid: String,
            user: User,
        ): Result<Unit> =
            try {
                firestore
                    .collection(User.COLLECTION_NAME)
                    .document(uid)
                    .set(user)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Aktualisiert die Team-ID eines Benutzers */
        suspend fun updateUserTeamId(
            uid: String,
            teamId: String,
        ): Result<Unit> =
            try {
                firestore
                    .collection(User.COLLECTION_NAME)
                    .document(uid)
                    .update(User.TEAM_ID_FIELD, teamId)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /**
         * Extrahiert den Namen aus einer E-Mail-Adresse z.B. "max.mustermann@example.com" -> "Max
         * Mustermann"
         */
        private fun extractNameFromEmail(email: String): String {
            val localPart = email.substringBefore("@")
            return localPart.split(".", "_", "-").joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }

        /** Erstellt einen Standard-Benutzer basierend auf E-Mail */
        fun createDefaultUser(email: String): User =
            User(
                name = extractNameFromEmail(email),
                email = email,
                role = User.ROLE_MANAGER, // Alle neuen User als Manager anlegen
                teamId = User.NO_TEAM, // Leerer String als initiale teamId
            )

        suspend fun updateUserTeamAndRole(
            userId: String,
            teamId: String,
            role: String,
        ): Result<Unit> =
            try {
                val updates =
                    mapOf(
                        User.TEAM_ID_FIELD to teamId,
                        User.ROLE_FIELD to role,
                    )
                firestore
                    .collection(User.COLLECTION_NAME)
                    .document(userId)
                    .update(updates)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Aktualisiert die Home-Location eines Benutzers für Geofencing */
        suspend fun updateUserHomeLocation(
            uid: String,
            latitude: Double,
            longitude: Double,
        ): Result<Unit> =
            try {
                val updates =
                    mapOf(
                        User.HOME_LATITUDE_FIELD to latitude,
                        User.HOME_LONGITUDE_FIELD to longitude,
                        User.HAS_HOME_LOCATION_FIELD to true,
                    )
                firestore
                    .collection(User.COLLECTION_NAME)
                    .document(uid)
                    .update(updates)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
    }
