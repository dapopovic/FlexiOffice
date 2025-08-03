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
         * Initializes FCM for the current user
         * Retrieves the current token and saves it to Firestore
         */
        fun initializeFCM() {
            firebaseMessaging.token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(TAG, "Error retrieving FCM token", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result

                val userId = auth.currentUser?.uid
                if (userId != null) {
                    saveTokenToFirestoreAsync(userId, token)
                    Log.d(TAG, "FCM Token for User $userId saved")
                } else {
                    Log.w(TAG, "No logged in user - Token not saved")
                }
            }
        }

        /**
         * Initializes FCM for the current user (suspend version)
         * Retrieves the current token and saves it to Firestore
         */
        suspend fun initializeFCMSuspend(): Result<String> =
            try {
                val token = firebaseMessaging.token.await()

                val userId = auth.currentUser?.uid
                if (userId != null) {
                    saveTokenToFirestore(userId, token)
                    Log.d(TAG, "FCM Token for User $userId saved")
                } else {
                    Log.w(TAG, "No logged in user - Token not saved")
                }

                Result.success(token)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing FCM", e)
                Result.failure(e)
            }

        /**
         * Saves the FCM token to Firestore for the user (async version)
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
                    Log.d(TAG, "FCM Token successfully saved to Firestore")
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Error saving FCM token", exception)
                    // Not critical - Token can be updated later
                }
        }

        /**
         * Save the FCM token to Firestore for the user (suspend version)
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

                Log.d(TAG, "FCM Token successfully saved to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving FCM token", e)
                // Not critical - Token can be updated later
            }
        }

        /**
         * Removes the FCM token on logout
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

                    Log.d(TAG, "FCM Token successfully removed")
                }

                // Firebase Messaging Token löschen
                firebaseMessaging.deleteToken().await()
                Log.d(TAG, "FCM Token completely deleted")

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting FCM token", e)
                Result.failure(e)
            }

        /**
         * Updates the token when a new one is generated
         */
        suspend fun updateToken(newToken: String): Result<Unit> =
            try {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    saveTokenToFirestore(userId, newToken)
                    Log.d(TAG, "FCM Token successfully updated for User $userId")
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating FCM token", e)
                Result.failure(e)
            }

        /**
         * Retrieves the current token without saving it
         */
        suspend fun getCurrentToken(): Result<String> =
            try {
                val token = firebaseMessaging.token.await()
                Result.success(token)
            } catch (e: Exception) {
                Log.e(TAG, "Error retrieving current token", e)
                Result.failure(e)
            }
    }
