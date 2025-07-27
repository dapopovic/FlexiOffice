package com.example.flexioffice.data

import com.example.flexioffice.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Repository für Benutzer-Datenoperationen in Firestore */
@Singleton
class UserRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
    ) {
        companion object {
            private const val USERS_COLLECTION = "users"
        }

        /** Erstellt einen neuen Benutzer in Firestore */
        suspend fun createUser(
            uid: String,
            user: User,
        ): Result<Unit> =
            try {
                firestore
                    .collection(USERS_COLLECTION)
                    .document(uid)
                    .set(user)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Lädt Benutzer-Daten aus Firestore */
        suspend fun getUser(uid: String): Result<User?> =
            try {
                val document =
                    firestore
                        .collection(USERS_COLLECTION)
                        .document(uid)
                        .get()
                        .await()

                val user = document.toObject(User::class.java)
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }

        suspend fun getUsersByTeamId(teamId: String): Result<List<User>> =
            try {
                val querySnapshot =
                    firestore
                        .collection(USERS_COLLECTION)
                        .whereEqualTo("teamId", teamId)
                        .get()
                        .await()

                val users = querySnapshot.toObjects(User::class.java)
                Result.success(users)
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
                    .collection(USERS_COLLECTION)
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
                    .collection(USERS_COLLECTION)
                    .document(uid)
                    .update("teamId", teamId)
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
    }
