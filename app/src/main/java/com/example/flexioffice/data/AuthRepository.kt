package com.example.flexioffice.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Repository für Firebase Authentication */
@Singleton
class AuthRepository @Inject constructor(private val firebaseAuth: FirebaseAuth) {

    /** Flow für den aktuellen Benutzer-Status */
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
        firebaseAuth.addAuthStateListener(authStateListener)

        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }

    /** Prüft ob ein Benutzer angemeldet ist */
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /** Anmeldung mit E-Mail und Passwort */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Anmeldung fehlgeschlagen"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Registrierung mit E-Mail und Passwort */
    suspend fun createUserWithEmailAndPassword(
            email: String,
            password: String
    ): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Registrierung fehlgeschlagen"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Abmeldung */
    fun signOut() {
        firebaseAuth.signOut()
    }

    /** Aktueller Benutzer */
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
}
