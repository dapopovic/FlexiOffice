package com.example.flexioffice.data

import com.example.flexioffice.data.model.Team
import com.example.flexioffice.data.model.User
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
    ) {
        companion object {
            const val TEAMS_COLLECTION = "teams"
        }

        /** Lädt ein Team anhand seiner ID */
        suspend fun getTeam(teamId: String): Result<Team?> =
            try {
                val team =
                    firestore
                        .collection(TEAMS_COLLECTION)
                        .document(teamId)
                        .get()
                        .await()
                        .toObject(Team::class.java)
                Result.success(team)
            } catch (e: Exception) {
                Result.failure(e)
            }


        /** Aktualisiert ein bestehendes Team */
        suspend fun updateTeam(team: Team): Result<Unit> =
            try {
                firestore
                    .collection(TEAMS_COLLECTION)
                    .document(team.id)
                    .set(team)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        fun getTeamStream(teamId: String): Flow<Result<Team>> =
            callbackFlow {
                val listenerRegistration =
                    firestore
                        .collection(TEAMS_COLLECTION)
                        .document(teamId)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(Result.failure(error))
                                return@addSnapshotListener
                            }
                            if (snapshot != null && snapshot.exists()) {
                                val team = snapshot.toObject(Team::class.java)
                                if (team != null) {
                                    trySend(Result.success(team))
                                } else {
                                    trySend(Result.failure(Exception("Failed to parse team data.")))
                                }
                            } else {
                                trySend(Result.failure(Exception("Team not found.")))
                            }
                        }
                awaitClose { listenerRegistration.remove() }
            }

        suspend fun createTeamAtomically(team: Team): Result<String> =
            try {
                val teamDocRef = firestore.collection(TEAMS_COLLECTION).document()
                val userDocRef = firestore.collection("users").document(team.managerId)

                // The team needs its own ID before being written
                val finalTeam = team.copy(id = teamDocRef.id)

                firestore
                    .runBatch { batch ->
                        // 1. Create the new team document
                        batch.set(teamDocRef, finalTeam)
                        // 2. Update the manager's user document with the new teamId
                        batch.update(userDocRef, "teamId", teamDocRef.id)
                    }.await()

                Result.success(teamDocRef.id)
            } catch (e: Exception) {
                Result.failure(e)
            }

        suspend fun inviteUserToTeamAtomically(
            teamId: String,
            managerId: String,
            invitedUserEmail: String,
        ): Result<Unit> =
            try {
                val userQuerySnapshot = findUserByEmail(invitedUserEmail)

                if (userQuerySnapshot.isEmpty) {
                    throw Exception("User with email $invitedUserEmail not found.")
                }
                val invitedUserDoc = userQuerySnapshot.documents.first()
                val invitedUserId = invitedUserDoc.id
                val invitedUserRef = invitedUserDoc.reference
                firestore
                    .runTransaction { transaction ->
                        val teamDocRef = firestore.collection(TEAMS_COLLECTION).document(teamId)
                        val teamSnapshot = transaction.get(teamDocRef)
                        val team =
                            teamSnapshot.toObject(Team::class.java)
                                ?: throw Exception("Team not found.")

                        val userSnapshot = transaction.get(invitedUserRef)
                        val invitedUser =
                            userSnapshot.toObject(User::class.java)
                                ?: throw Exception("Could not parse invited user data.")

                        // Security Check: Ensure the person inviting is the manager
                        if (team.managerId != managerId) {
                            throw Exception("Only the team manager can invite new members.")
                        }
                        // Check if the user is already in a team
                        if (invitedUser.teamId.isNotEmpty()) {
                            throw Exception("This user is already in a team.")
                        }

                        // All checks passed, perform the atomic updates
                        transaction.update(invitedUserRef, "teamId", teamId, "role", User.ROLE_USER)
                        transaction.update(teamDocRef, "members", FieldValue.arrayUnion(invitedUserId))

                        // The transaction will automatically commit. A return value is not needed.
                    }.await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Aktualisiert die Mitgliederliste eines Teams */
        suspend fun updateTeamMembers(
            teamId: String,
            members: List<String>,
        ): Result<Unit> =
            try {
                firestore
                    .collection(TEAMS_COLLECTION)
                    .document(teamId)
                    .update("members", members)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Prüft, ob ein Benutzer Manager eines Teams ist */
        suspend fun isTeamManager(
            userId: String,
            teamId: String,
        ): Result<Boolean> =
            try {
                val team = getTeam(teamId).getOrNull()
                Result.success(team?.managerId == userId)
            } catch (e: Exception) {
                Result.failure(e)
            }

        suspend fun findUserByEmail(email: String): QuerySnapshot =
            firestore
                .collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
    }
