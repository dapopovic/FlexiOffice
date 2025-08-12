package com.example.flexioffice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.fcm.FCMTokenManager
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = true,
    val user: FirebaseUser? = null,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
)

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val fmcTokenManager: FCMTokenManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AuthUiState())
        val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

        init {
            // monitoring the authentication status
            viewModelScope.launch {
                authRepository.currentUser.collect { user ->
                    _uiState.value =
                        _uiState.value.copy(
                            user = user,
                            isLoggedIn = user != null,
                            isLoading = false,
                        )
                }
            }
        }

        /** Login with E-Mail and Password */
        fun signInWithEmailAndPassword(
            email: String,
            password: String,
        ) {
            if (email.isBlank() || password.isBlank()) {
                _uiState.value =
                    _uiState.value.copy(errorMessage = "E-Mail und Passwort dürfen nicht leer sein")
                return
            }

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                authRepository
                    .signInWithEmailAndPassword(email, password)
                    .onSuccess { user ->
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                user = user,
                                isLoggedIn = true,
                                errorMessage = null,
                            )
                        // Initialize FCM token for this user (non-blocking)
                        viewModelScope.launch { fmcTokenManager.initializeFCMSuspend() }
                    }.onFailure { exception ->
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                errorMessage =
                                    exception.message
                                        ?: "Anmeldung fehlgeschlagen",
                            )
                    }
            }
        }

        /** Registration with E-Mail and Password */
        fun createUserWithEmailAndPassword(
            email: String,
            password: String,
        ) {
            if (email.isBlank() || password.isBlank()) {
                _uiState.value =
                    _uiState.value.copy(errorMessage = "E-Mail und Passwort dürfen nicht leer sein")
                return
            }

            if (password.length < 6) {
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = "Passwort muss mindestens 6 Zeichen lang sein",
                    )
                return
            }

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                authRepository
                    .createUserWithEmailAndPassword(email, password)
                    .onSuccess { user ->
                        // User successfully created in Firebase Auth
                        // Now create user document in Firestore
                        createUserInFirestore(user.uid, email)
                    }.onFailure { exception ->
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                errorMessage =
                                    exception.message
                                        ?: "Registrierung fehlgeschlagen",
                            )
                    }
            }
        }

        /** Sign out */
        fun signOut() {
            authRepository.signOut()
            _uiState.value = AuthUiState() // Reset state
        }

        /** Clear error message */
        fun clearErrorMessage() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }

        /** Creates user document in Firestore after successful registration */
        private suspend fun createUserInFirestore(
            uid: String,
            email: String,
        ) {
            val defaultUser = userRepository.createDefaultUser(email)

            userRepository
                .createUser(uid, defaultUser)
                .onSuccess {
                    // Firestore document successfully created
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            user = authRepository.getCurrentUser(),
                            isLoggedIn = true,
                            errorMessage = null,
                        )
                    // Initialize FCM token for the newly registered user (non-blocking)
                    viewModelScope.launch { fmcTokenManager.initializeFCMSuspend() }
                }.onFailure { exception ->
                    // Firestore document creation failed
                    // User is created in Firebase Auth but not in Firestore
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage =
                                "Benutzer erstellt, aber Profil konnte nicht gespeichert werden: ${exception.message}",
                        )
                }
        }
    }
