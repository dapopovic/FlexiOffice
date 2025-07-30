package com.example.flexioffice.fcm

import android.util.Log
import com.example.flexioffice.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMTokenManager
    @Inject
    constructor(
        private val firebaseMessaging: FirebaseMessaging,
        private val firestore: FirebaseFirestore,
        private val auth: FirebaseAuth,
    ) {
        companion object {
            private const val TAG = "FCMTokenManager"
            private const val FCM_TOKEN_FIELD = "fcmToken"
        }

        /**
         * Initialisiert FCM für den aktuellen Benutzer
         * Holt das aktuelle Token und speichert es in Firestore
         */
        fun initializeFCM() {
            firebaseMessaging.token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(TAG, "Fehler beim Abrufen des FCM Tokens", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d(TAG, "FCM Token erhalten")

                val userId = auth.currentUser?.uid
                if (userId != null) {
                    saveTokenToFirestoreAsync(userId, token)
                    Log.d(TAG, "FCM Token für User $userId gespeichert")
                } else {
                    Log.w(TAG, "Kein angemeldeter Benutzer - Token nicht gespeichert")
                }
            }
        }

        /**
         * Initialisiert FCM für den aktuellen Benutzer (suspend version)
         * Holt das aktuelle Token und speichert es in Firestore
         */
        suspend fun initializeFCMSuspend(): Result<String> =
            try {
                val token = firebaseMessaging.token.await()
                Log.d(TAG, "FCM Token erhalten")

                val userId = auth.currentUser?.uid
                if (userId != null) {
                    saveTokenToFirestore(userId, token)
                    Log.d(TAG, "FCM Token für User $userId gespeichert")
                } else {
                    Log.w(TAG, "Kein angemeldeter Benutzer - Token nicht gespeichert")
                }

                Result.success(token)
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Initialisieren von FCM", e)
                Result.failure(e)
            }

        /**
         * Speichert das FCM Token in Firestore für den Benutzer (async version)
         */
        private fun saveTokenToFirestoreAsync(
            userId: String,
            token: String,
        ) {
            firestore
                .collection(User.COLLECTION_NAME)
                .document(userId)
                .set(mapOf(FCM_TOKEN_FIELD to token), SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "FCM Token erfolgreich in Firestore gespeichert")
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Fehler beim Speichern des FCM Tokens", exception)
                    // Nicht kritisch - Token kann später aktualisiert werden
                }
        }

        /**
         * Speichert das FCM Token in Firestore für den Benutzer
         */
        private suspend fun saveTokenToFirestore(
            userId: String,
            token: String,
        ) {
            try {
                firestore
                    .collection(User.COLLECTION_NAME)
                    .document(userId)
                    .set(mapOf(FCM_TOKEN_FIELD to token), SetOptions.merge())
                    .await()

                Log.d(TAG, "FCM Token erfolgreich in Firestore gespeichert")
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Speichern des FCM Tokens", e)
                // Nicht kritisch - Token kann später aktualisiert werden
            }
        }

        /**
         * Entfernt das FCM Token beim Logout
         */
        suspend fun clearToken(): Result<Unit> =
            try {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    firestore
                        .collection(User.COLLECTION_NAME)
                        .document(userId)
                        .set(mapOf(FCM_TOKEN_FIELD to null), SetOptions.merge())
                        .await()

                    Log.d(TAG, "FCM Token erfolgreich entfernt")
                }

                // Firebase Messaging Token löschen
                firebaseMessaging.deleteToken().await()
                Log.d(TAG, "FCM Token komplett gelöscht")

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Löschen des FCM Tokens", e)
                Result.failure(e)
            }

        /**
         * Aktualisiert das Token wenn sich ein neues generiert
         */
        suspend fun updateToken(newToken: String): Result<Unit> =
            try {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    saveTokenToFirestore(userId, newToken)
                    Log.d(TAG, "FCM Token erfolgreich aktualisiert")
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Aktualisieren des FCM Tokens", e)
                Result.failure(e)
            }

        /**
         * Holt das aktuelle Token ohne es zu speichern
         */
        suspend fun getCurrentToken(): Result<String> =
            try {
                val token = firebaseMessaging.token.await()
                Result.success(token)
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Abrufen des aktuellen Tokens", e)
                Result.failure(e)
            }
    }
