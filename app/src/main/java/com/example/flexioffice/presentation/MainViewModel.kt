package com.example.flexioffice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.data.model.User
import com.example.flexioffice.geofencing.GeofencingInitializer
import com.example.flexioffice.navigation.BottomNavigationItem
import com.example.flexioffice.navigation.BottomNavigationItems
import com.example.flexioffice.navigation.FlexiOfficeRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null,
    val userRole: String = User.ROLE_USER,
    val availableNavItems: List<BottomNavigationItem> =
        emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val geofencingInitializer: GeofencingInitializer,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MainUiState())
        val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                authRepository.currentUser
                    .map { it?.uid }
                    .distinctUntilChanged()
                    .flatMapLatest { userId ->
                        if (userId == null) {
                            // User is logged out, clean up geofencing and emit the logged-out state.
                            geofencingInitializer.cleanupGeofencingOnLogout()
                            flowOf(
                                MainUiState(isLoading = false, currentUser = null),
                            )
                        } else {
                            // User is logged in, get their data stream.
                            userRepository.getUserStream(userId).map { userResult ->
                                userResult
                                    .map { user ->
                                        // Success: We got the user data.
                                        val navItems = BottomNavigationItems.getItemsForUser(user)

                                        // Initialize geofencing for logged-in user
                                        geofencingInitializer.initializeGeofencingOnAppStart()

                                        MainUiState(
                                            isLoading = false,
                                            currentUser = user,
                                            availableNavItems = navItems,
                                        )
                                    }.getOrElse {
                                        // Failure: Could not get user profile from Firestore.
                                        // Still logged in, but fall back to a default role without team.

                                        // Initialize geofencing even with fallback user data
                                        geofencingInitializer.initializeGeofencingOnAppStart()

                                        val navItems = BottomNavigationItems.getItemsForUser(null)
                                        MainUiState(
                                            isLoading = false,
                                            currentUser = null, // Or a default User object
                                            availableNavItems = navItems,
                                        )
                                    }
                            }
                        }
                    }.catch {
                        // Catch any unexpected errors in the flow itself
                        emit(MainUiState(isLoading = false))
                    }.collect { state -> _uiState.value = state }
            }
        }

        /** Prüft ob der aktuelle Benutzer Zugriff auf eine Route hat */
        fun hasAccessToRoute(route: String): Boolean {
            val currentState = _uiState.value
            return currentState.availableNavItems.any { it.route == route }
        }

        /** Prüft ob der aktuelle Benutzer Teil eines Teams ist */
        fun hasTeamMembership(): Boolean {
            val currentUser = _uiState.value.currentUser
            return currentUser?.teamId?.isNotEmpty() == true && currentUser.teamId != User.NO_TEAM
        }

        /** Gibt die Standard-Route für den aktuellen Benutzer zurück */
        fun getDefaultRoute(): String {
            val availableItems = _uiState.value.availableNavItems
            return availableItems.firstOrNull()?.route ?: FlexiOfficeRoutes.Loading.route
        }
    }
