package edu.utap.auth

import com.google.firebase.auth.FirebaseUser
import edu.utap.auth.model.FirestoreUser // Keep existing import, might be needed elsewhere
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

    // StateFlow to expose the current Firebase user
    private val _currentUser = MutableStateFlow<FirestoreUser?>(null)
    val currentUser: StateFlow<FirestoreUser?> = _currentUser

    /**
     * Updates the authentication state and current user.
     * @param state The new authentication state
     * @param user The current Firebase user (optional)
     */
    fun updateState(state: AuthState, user: FirestoreUser? = null) {
        _authState.value = state
        _currentUser.value = user
    }

    /**
     * Updates only the current Firebase user.
     * @param user The current Firebase user
     */
    fun updateCurrentUser(user: FirestoreUser) {
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
