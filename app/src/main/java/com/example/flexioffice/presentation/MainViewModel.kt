package com.example.flexioffice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.data.model.User
import com.example.flexioffice.navigation.BottomNavigationItems
import com.example.flexioffice.navigation.FlexiOfficeRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null,
    val userRole: String = User.ROLE_USER,
    val availableNavItems: List<com.example.flexioffice.navigation.BottomNavigationItem> =
        emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MainUiState())
        val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

        init {
            observeAuthState()
        }

        /** Öffentliche Methode zum Neuladen der Benutzerdaten */
    fun refreshUserData() {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser.first()
            if (currentUser != null) {
                loadUserData(currentUser.uid)
            }
        }
    }

    private fun observeAuthState() {
            viewModelScope.launch {
                authRepository.currentUser.collect { firebaseUser ->
                    if (firebaseUser != null) {
                        // Benutzer ist angemeldet, lade Firestore-Daten
                        loadUserData(firebaseUser.uid)
                    } else {
                        // Benutzer ist nicht angemeldet
                        _uiState.value = MainUiState(isLoggedIn = false, isLoading = false)
                    }
                }
            }
        }

        private suspend fun loadUserData(uid: String) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            userRepository
                .getUser(uid)
                .onSuccess { user ->
                    val userRole = user?.role ?: User.ROLE_USER
                    val navItems = BottomNavigationItems.getItemsForRole(userRole)

                    _uiState.value =
                        _uiState.value.copy(
                            isLoggedIn = true,
                            currentUser = user,
                            userRole = userRole,
                            availableNavItems = navItems,
                            isLoading = false,
                        )
                }.onFailure { exception ->
                    // Fallback: Standard-Benutzer-Rolle verwenden
                    val navItems = BottomNavigationItems.getItemsForRole(User.ROLE_USER)

                    _uiState.value =
                        _uiState.value.copy(
                            isLoggedIn = true,
                            currentUser = null,
                            userRole = User.ROLE_USER,
                            availableNavItems = navItems,
                            isLoading = false,
                        )
                }
        }

        /** Prüft ob der aktuelle Benutzer Zugriff auf eine Route hat */
        fun hasAccessToRoute(route: String): Boolean {
            val currentState = _uiState.value
            return currentState.availableNavItems.any { it.route == route }
        }

        /** Gibt die Standard-Route für den aktuellen Benutzer zurück */
        fun getDefaultRoute(): String {
            val availableItems = _uiState.value.availableNavItems
            return availableItems.firstOrNull()?.route ?: FlexiOfficeRoutes.Calendar.route
        }
    }
