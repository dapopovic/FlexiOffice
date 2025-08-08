package com.example.flexioffice.data

import android.util.Log
import com.example.flexioffice.data.model.Team
import com.example.flexioffice.data.model.TeamInvitation
import com.example.flexioffice.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val auth: FirebaseAuth,
    ) {
        /** Loads the current user's team */
        suspend fun getCurrentUserTeam(): Team? {
            val uid = auth.currentUser?.uid ?: return null
            val userDoc =
                firestore
                    .collection(User.COLLECTION_NAME)
                    .document(uid)
                    .get()
                    .await()
            val teamId = userDoc.getString(User.TEAM_ID_FIELD) ?: return null
            return getTeam(teamId).getOrNull()
        }

        /** Loads a team by its ID */
        suspend fun getTeam(teamId: String): Result<Team?> =
            try {
                val team =
                    firestore
                        .collection(Team.COLLECTION_NAME)
                        .document(teamId)
                        .get()
                        .await()
                        .toObject(Team::class.java)
                Result.success(team)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Updates an existing team */
        suspend fun updateTeam(team: Team): Result<Unit> =
            try {
                firestore
                    .collection(Team.COLLECTION_NAME)
                    .document(team.id)
                    .set(team)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Streams updates for a specific team */
        fun getTeamStream(teamId: String): Flow<Result<Team>> =
            callbackFlow {
                val listenerRegistration =
                    firestore
                        .collection(Team.COLLECTION_NAME)
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

        /** Creates a new team atomically */
        suspend fun createTeamAtomically(team: Team): Result<String> =
            try {
                val teamDocRef = firestore.collection(Team.COLLECTION_NAME).document()
                val userDocRef = firestore.collection(User.COLLECTION_NAME).document(team.managerId)

                // The team needs its own ID before being written
                val finalTeam = team.copy(id = teamDocRef.id)

                firestore
                    .runBatch { batch ->
                        // 1. Create the new team document
                        batch.set(teamDocRef, finalTeam)
                        // 2. Update the manager's user document with the new teamId
                        batch.update(userDocRef, User.TEAM_ID_FIELD, teamDocRef.id)
                    }.await()

                Result.success(teamDocRef.id)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Creates a team invitation instead of directly adding the user */
        suspend fun createTeamInvitation(
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
                val invitedUser =
                    invitedUserDoc.toObject(User::class.java)
                        ?: throw Exception("Could not parse invited user data.")

                // Get current user info for invitation
                val currentUserDoc =
                    firestore
                        .collection(User.COLLECTION_NAME)
                        .document(managerId)
                        .get()
                        .await()
                val currentUser =
                    currentUserDoc.toObject(User::class.java)
                        ?: throw Exception("Manager user not found.")

                // Get team info for invitation
                val teamDoc =
                    firestore
                        .collection(Team.COLLECTION_NAME)
                        .document(teamId)
                        .get()
                        .await()
                val team =
                    teamDoc.toObject(Team::class.java)
                        ?: throw Exception("Team not found.")

                // Security Check: Ensure the person inviting is the manager
                if (team.managerId != managerId) {
                    throw Exception("Only the team manager can invite new members.")
                }

                // Check if the user is already in a team
                if (invitedUser.teamId.isNotEmpty()) {
                    throw Exception("This user is already in a team.")
                }

                // Check if there's already a pending invitation for this user to this team
                val existingInvitations =
                    firestore
                        .collection(TeamInvitation.COLLECTION_NAME)
                        .whereEqualTo(TeamInvitation.TEAM_ID_FIELD, teamId)
                        .whereEqualTo(TeamInvitation.INVITED_USER_ID_FIELD, invitedUserId)
                        .whereEqualTo(TeamInvitation.STATUS_FIELD, TeamInvitation.STATUS_PENDING)
                        .get()
                        .await()

                if (!existingInvitations.isEmpty) {
                    throw Exception("There is already a pending invitation for this user to this team.")
                }

                // Create the invitation
                val invitation =
                    TeamInvitation(
                        id = UUID.randomUUID().toString(),
                        teamId = teamId,
                        teamName = team.name,
                        invitedByUserId = managerId,
                        invitedByUserName = currentUser.name,
                        invitedUserEmail = invitedUserEmail,
                        invitedUserId = invitedUserId,
                        status = TeamInvitation.STATUS_PENDING,
                        createdAt = Date(),
                    )

                firestore
                    .collection(TeamInvitation.COLLECTION_NAME)
                    .document(invitation.id)
                    .set(invitation)
                    .await()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Updates the member list of a team */
        suspend fun updateTeamMembers(
            teamId: String,
            members: List<String>,
        ): Result<Unit> =
            try {
                firestore
                    .collection(Team.COLLECTION_NAME)
                    .document(teamId)
                    .update(Team.MEMBERS_FIELD, members)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Checks if a user is the manager of a team */
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

        /** Finds a user by their email */
        suspend fun findUserByEmail(email: String): QuerySnapshot =
            firestore
                .collection(User.COLLECTION_NAME)
                .whereEqualTo(User.EMAIL_FIELD, email)
                .limit(1)
                .get()
                .await()

        /** Gets pending team invitations for the current user */
        suspend fun getPendingInvitations(): Result<List<TeamInvitation>> =
            try {
                val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")

                val invitations =
                    firestore
                        .collection(TeamInvitation.COLLECTION_NAME)
                        .whereEqualTo(TeamInvitation.INVITED_USER_ID_FIELD, uid)
                        .whereEqualTo(TeamInvitation.STATUS_FIELD, TeamInvitation.STATUS_PENDING)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.toObject(TeamInvitation::class.java) }

                Result.success(invitations)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Listens for pending invitations in real-time */
        fun getPendingInvitationsFlow(): Flow<Result<List<TeamInvitation>>> =
            callbackFlow {
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    trySend(Result.success(emptyList()))
                    awaitClose {}
                    return@callbackFlow
                }

                val listenerRegistration =
                    firestore
                        .collection(TeamInvitation.COLLECTION_NAME)
                        .whereEqualTo(TeamInvitation.INVITED_USER_ID_FIELD, uid)
                        .whereEqualTo(TeamInvitation.STATUS_FIELD, TeamInvitation.STATUS_PENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(Result.failure(error))
                                return@addSnapshotListener
                            }

                            val invitations =
                                snapshot?.documents?.mapNotNull {
                                    it.toObject(TeamInvitation::class.java)
                                } ?: emptyList()

                            trySend(Result.success(invitations))
                        }

                awaitClose { listenerRegistration.remove() }
            }

        /** Listens for pending invitations for a given team (outgoing, visible to manager) */
        fun getTeamPendingInvitationsFlow(teamId: String): Flow<Result<List<TeamInvitation>>> =
            callbackFlow {
                val listenerRegistration =
                    firestore
                        .collection(TeamInvitation.COLLECTION_NAME)
                        .whereEqualTo(TeamInvitation.TEAM_ID_FIELD, teamId)
                        .whereEqualTo(TeamInvitation.STATUS_FIELD, TeamInvitation.STATUS_PENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(Result.failure(error))
                                return@addSnapshotListener
                            }

                            val invitations =
                                snapshot?.documents?.mapNotNull {
                                    it.toObject(TeamInvitation::class.java)
                                } ?: emptyList()

                            trySend(Result.success(invitations))
                        }

                awaitClose { listenerRegistration.remove() }
            }

        /** Accepts a team invitation */
        suspend fun acceptTeamInvitation(invitationId: String): Result<Unit> =
            try {
                val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")

                firestore
                    .runTransaction { transaction ->
                        val invitationRef = firestore.collection(TeamInvitation.COLLECTION_NAME).document(invitationId)
                        val invitationSnapshot = transaction.get(invitationRef)
                        val invitation =
                            invitationSnapshot.toObject(TeamInvitation::class.java)
                                ?: throw Exception("Invitation not found.")

                        // Verify this invitation is for the current user
                        if (invitation.invitedUserId != uid) {
                            throw Exception("This invitation is not for you.")
                        }

                        // Verify invitation is still pending
                        if (invitation.status != TeamInvitation.STATUS_PENDING) {
                            throw Exception("This invitation has already been responded to.")
                        }

                        // Check if user is still available (not in a team)
                        val userRef = firestore.collection(User.COLLECTION_NAME).document(uid)
                        val userSnapshot = transaction.get(userRef)
                        val user =
                            userSnapshot.toObject(User::class.java)
                                ?: throw Exception("User not found.")

                        if (user.teamId.isNotEmpty()) {
                            throw Exception("You are already in a team.")
                        }

                        // Update invitation status
                        transaction.update(
                            invitationRef,
                            TeamInvitation.STATUS_FIELD,
                            TeamInvitation.STATUS_ACCEPTED,
                            TeamInvitation.RESPONDED_AT_FIELD,
                            Date(),
                        )

                        // Add user to team
                        val teamRef = firestore.collection(Team.COLLECTION_NAME).document(invitation.teamId)
                        transaction.update(
                            userRef,
                            User.TEAM_ID_FIELD,
                            invitation.teamId,
                            User.ROLE_FIELD,
                            User.ROLE_USER,
                        )
                        transaction.update(teamRef, Team.MEMBERS_FIELD, FieldValue.arrayUnion(uid))
                    }.await()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Declines a team invitation */
        suspend fun declineTeamInvitation(invitationId: String): Result<Unit> =
            try {
                val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")

                firestore
                    .runTransaction { transaction ->
                        val invitationRef = firestore.collection(TeamInvitation.COLLECTION_NAME).document(invitationId)
                        val invitationSnapshot = transaction.get(invitationRef)
                        val invitation =
                            invitationSnapshot.toObject(TeamInvitation::class.java)
                                ?: throw Exception("Invitation not found.")

                        // Verify this invitation is for the current user
                        if (invitation.invitedUserId != uid) {
                            throw Exception("This invitation is not for you.")
                        }

                        // Verify invitation is still pending
                        if (invitation.status != TeamInvitation.STATUS_PENDING) {
                            throw Exception("This invitation has already been responded to.")
                        }

                        // Update invitation status
                        transaction.update(
                            invitationRef,
                            TeamInvitation.STATUS_FIELD,
                            TeamInvitation.STATUS_DECLINED,
                            TeamInvitation.RESPONDED_AT_FIELD,
                            Date(),
                        )

                        // Restore manager role for declined user so they can create teams again
                        val userRef = firestore.collection(User.COLLECTION_NAME).document(uid)
                        transaction.update(userRef, User.ROLE_FIELD, User.ROLE_MANAGER)
                    }.await()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        /** Cancels a pending team invitation (manager only). */
        suspend fun cancelTeamInvitation(invitationId: String): Result<Unit> =
            try {
                val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")

                firestore
                    .runTransaction { transaction ->
                        val invitationRef = firestore.collection(TeamInvitation.COLLECTION_NAME).document(invitationId)
                        val invitationSnapshot = transaction.get(invitationRef)
                        val invitation =
                            invitationSnapshot.toObject(TeamInvitation::class.java)
                                ?: throw Exception("Invitation not found.")

                        // Only manager who created it can cancel, and only when pending
                        if (invitation.invitedByUserId != uid) {
                            throw Exception("Keine Berechtigung, diese Einladung zu stornieren.")
                        }
                        if (invitation.status != TeamInvitation.STATUS_PENDING) {
                            throw Exception("Einladung ist nicht mehr ausstehend.")
                        }

                        // Simpler approach: delete the invitation document
                        transaction.delete(invitationRef)
                    }.await()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
    }
