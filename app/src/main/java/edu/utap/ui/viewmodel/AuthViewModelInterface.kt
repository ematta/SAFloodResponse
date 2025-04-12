package edu.utap.ui.viewmodel

import edu.utap.auth.AuthState
import edu.utap.models.FirestoreUser
import edu.utap.utils.RoleUtils
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for AuthViewModel to allow for easier testing
 */
/**
 * Interface defining authentication-related ViewModel operations.
 *
 * This abstraction allows for easier testing and mocking of authentication logic.
 */
interface AuthViewModelInterface {
    /**
     * Current authentication state exposed as a [StateFlow].
     */
    val authState: StateFlow<AuthState>

    /**
     * Registers a new user with email, password, name, and role.
     *
     * @param email User's email.
     * @param password User's password.
     * @param name User's display name.
     * @param role User role (default: regular).
     * @param function Callback with success flag and optional message.
     */
    fun register(
        email: String,
        password: String,
        name: String,
        role: String = RoleUtils.ROLE_REGULAR,
        function: (boolean: Boolean, message: String?) -> Unit = { _, _ -> }
    )

    /**
     * Logs in an existing user.
     *
     * @param email User's email.
     * @param password User's password.
     * @param function Callback with success flag and optional message.
     */
    fun login(
        email: String,
        password: String,
        function: (boolean: Boolean, message: String?) -> Unit
    )

    /**
     * Logs out the current user.
     *
     * @return The unauthenticated state.
     */
    fun logout(): AuthState.Idle.Unauthenticated

    /**
     * Sends a password reset email.
     *
     * @param email User's email.
     * @param callback Callback with success flag and optional message.
     */
    fun resetPassword(email: String, callback: (Boolean, String?) -> Unit)

    /**
     * Gets the current authenticated user profile, if any.
     *
     * @return The [edu.utap.models.FirestoreUser] or null if not authenticated.
     */
    fun getCurrentUser(): FirestoreUser?

    /**
     * Restores the authentication state, e.g., after app restart.
     */
    fun restoreAuthState()

    /**
     * Updates the current authentication state.
     *
     * @param sent The new [AuthState].
     */
    fun updateAuthState(sent: AuthState)

    /**
     * Checks to see if user is logged in and authenticated
     */
    fun isUserLoggedIn(): Boolean
}
