package edu.utap.auth

import edu.utap.auth.db.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages authentication state for the application.
 * This class handles the state flows for both authentication state and current user.
 */
class AuthStateManager {
    // StateFlow to expose authentication state to UI components
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle.Initial)
    val authState: StateFlow<AuthState> = _authState

    // StateFlow to expose the current user entity
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    /**
     * Updates the authentication state and current user.
     * @param state The new authentication state
     * @param user The current user entity (optional)
     */
    fun updateState(state: AuthState, user: UserEntity? = null) {
        _authState.value = state
        _currentUser.value = user
    }

    /**
     * Updates only the current user.
     * @param user The current user entity
     */
    fun updateCurrentUser(user: UserEntity?) {
        _currentUser.value = user
    }

    /**
     * Resets the state to unauthenticated and clears the current user.
     */
    fun resetState() {
        _authState.value = AuthState.Idle.Unauthenticated
        _currentUser.value = null
    }
} 