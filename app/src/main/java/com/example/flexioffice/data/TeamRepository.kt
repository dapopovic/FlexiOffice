package com.example.flexioffice.data

import com.example.flexioffice.data.model.Team
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepository @Inject constructor(private val firestore: FirebaseFirestore) {

    companion object {
        const val TEAMS_COLLECTION = "teams"
    }

    /** Erstellt ein neues Team-Dokument in Firestore */
    suspend fun createTeam(team: Team): Result<String> {
        return try {
            val documentRef = firestore.collection(TEAMS_COLLECTION).document()
            documentRef.set(team).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Lädt ein Team anhand seiner ID */
    suspend fun getTeam(teamId: String): Result<Team?> {
        return try {
            val team = firestore.collection(TEAMS_COLLECTION)
                .document(teamId)
                .get()
                .await()
                .toObject(Team::class.java)
            Result.success(team)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Aktualisiert die Mitgliederliste eines Teams */
    suspend fun updateTeamMembers(teamId: String, members: List<String>): Result<Unit> {
        return try {
            firestore.collection(TEAMS_COLLECTION)
                .document(teamId)
                .update("members", members)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Prüft, ob ein Benutzer Manager eines Teams ist */
    suspend fun isTeamManager(userId: String, teamId: String): Result<Boolean> {
        return try {
            val team = getTeam(teamId).getOrNull()
            Result.success(team?.managerId == userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
