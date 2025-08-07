package com.example.flexioffice.data

import android.util.Log
import com.example.flexioffice.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Repository for user data operations in Firestore */
@Singleton
class UserRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val auth: FirebaseAuth,
    ) {
        /** Creates a new user in Firestore */
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

        /** Loads user data from Firestore */
        suspend fun getCurrentUser(): User? {
            val uid = auth.currentUser?.uid ?: return null
            return getUser(uid).getOrNull()
        }

        /** Loads a user by their UID */
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

        /** Loads a user by their ID */
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

        /** Gets a list of users by team ID */
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

        /** Gets a stream of user data */
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

        /** Gets a stream of team members */
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

        /** Removes a member from a team by resetting the teamId */
        suspend fun removeUserFromTeam(userId: String): Boolean =
            try {
                // When removing user from team, restore initial permissions (manager role)
                // so they can create new teams if needed
                val updates =
                    mapOf(
                        "teamId" to "",
                        "role" to User.ROLE_MANAGER,
                    )

                firestore
                    .collection("users")
                    .document(userId)
                    .update(updates)
                    .await()
                true
            } catch (e: Exception) {
                Log.e("UserRepository", "Error removing user from team", e)
                false
            }

        /** Updates user data in Firestore */
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

        /** Updates the team ID of a user */
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
         * Extracts the name from an email address e.g. "max.mustermann@example.com" -> "Max
         * Mustermann"
         */
        private fun extractNameFromEmail(email: String): String {
            val localPart = email.substringBefore("@")
            return localPart.split(".", "_", "-").joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }

        /** Creates a default user based on email */
        fun createDefaultUser(email: String): User =
            User(
                name = extractNameFromEmail(email),
                email = email,
                role = User.ROLE_MANAGER, // Every new user is a manager by default
                teamId = User.NO_TEAM, // Empty string as initial teamId
            )

        /** Updates the team and role of a user */
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

        /** Updates the home location of a user for geofencing */
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
