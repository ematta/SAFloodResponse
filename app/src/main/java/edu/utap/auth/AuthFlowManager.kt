package edu.utap.auth

import edu.utap.ui.viewmodel.AuthViewModel
import edu.utap.ui.viewmodel.UserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages authentication flow, state restoration, and exposes authentication state.
 * Delegates actual auth operations to AuthViewModel.
 */
class AuthFlowManager(val authViewModel: AuthViewModel) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        authViewModel.restoreAuthState()

        val cachedUser = authViewModel.getCachedUser()
        val initialIsAuthenticated = cachedUser != null && !authViewModel.isAuthExpired()
        if (!initialIsAuthenticated) {
            authViewModel.clearAuthCache()
        }

        _isAuthenticated.value = initialIsAuthenticated

        scope.launch {
            authViewModel.authState.collect { state ->
                val authenticated = state is AuthState.Idle.Authenticated
                _isAuthenticated.value = authenticated
            }
        }
    }

    /**
     * Initiates the login process using the provided email and password.
     *
     * This function delegates the actual login logic to the `authViewModel`.
     * It takes an email, password, and a callback function as parameters.
     * The callback function will be invoked with the result of the login attempt.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @param callback A lambda function that will be called when the login attempt completes.
     *                 It receives two parameters:
     *                 - A `Boolean` indicating whether the login was successful (`true`) or not (`false`).
     *                 - An optional `String` representing an error message if the login failed, or `null` if it succeeded.
     *
     * Example Usage:
     * ```kotlin
     * login("user@example.com", "password123") { success, errorMessage ->
     *     if (success) {
     *         // Login successful
     *         println("Login successful!")
     *     } else {
     *         // Login failed
     *         println("Login failed: $errorMessage")
     *     }
     * }
     * ```
     */
    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        authViewModel.login(email, password, callback)
    }

    /**
     * Registers a new user with the specified email, password, name, and role.
     *
     * This function delegates the registration process to the `authViewModel`.
     * It takes the user's information (email, password, name, role) and a callback function.
     * The callback function will be invoked after the registration attempt with the result.
     *
     * @param email The email address of the user to be registered.
     * @param password The password of the user to be registered.
     * @param name The name of the user to be registered.
     * @param role The role of the user to be registered (e.g., "user", "admin").
     * @param callback A function that will be called with the result of the registration attempt.
     *                 The first parameter (Boolean) indicates whether the registration was successful (true) or not (false).
     *                 The second parameter (String?) contains an error message if the registration failed, or null if it succeeded.
     *
     * Example Usage:
     * ```
     * register("test@example.com", "password123", "Test User", "user") { success, error ->
     *     if (success) {
     *         println("Registration successful!")
     *     } else {
     *         println("Registration failed: $error")
     *     }
     * }
     * ```
     */
    fun register(
        email: String,
        password: String,
        name: String,
        role: String,
        callback: (Boolean, String?) -> Unit
    ) {
        authViewModel.register(email, password, name, role, callback)
    }

    /**
     * Logs the user out of the application.
     *
     * This function delegates the actual logout process to the `authViewModel`.
     * It typically involves clearing user authentication tokens, session data,
     * and potentially navigating the user to a login screen.
     *
     * @see [ViewModel.logout] for the underlying implementation.
     */
    fun logout() {
        authViewModel.logout()
    }

    /**
     * Resets the password for a user associated with the given email address.
     *
     * This function delegates the password reset operation to the `authViewModel`.
     *
     * @param email The email address of the user requesting a password reset.
     * @param callback A callback function that will be invoked with the result of the operation.
     *                 - The first parameter (Boolean) indicates whether the password reset request was successful.
     *                   `true` if successful, `false` otherwise.
     *                 - The second parameter (String?) provides an optional error message if the operation failed.
     *                   It will be `null` if the operation was successful.
     *
     * Example Usage:
     * ```
     * resetPassword("user@example.com") { success, errorMessage ->
     *     if (success) {
     *         println("Password reset email sent successfully.")
     *     } else {
     *         println("Password reset failed: $errorMessage")
     *     }
     * }
     * ```
     */
    fun resetPassword(email: String, callback: (Boolean, String?) -> Unit) {
        authViewModel.resetPassword(email, callback)
    }

    /**
     * Clears the authentication cache.
     *
     * This function delegates the task of clearing the authentication cache to the
     * `authViewModel`. This typically involves removing any stored tokens, user
     * information, or other credentials that are cached locally.
     *
     * Use this function when a user logs out, or when it's necessary to force a
     * fresh authentication flow, such as after a password change or when dealing
     * with expired tokens.  It ensures that the application doesn't rely on
     * outdated or invalid authentication data.
     *
     * @see AuthViewModel.clearAuthCache for details on the underlying implementation.
     */
    fun clearAuthCache() {
        authViewModel.clearAuthCache()
    }
}
