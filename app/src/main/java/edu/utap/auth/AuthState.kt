package edu.utap.auth

import com.google.firebase.auth.FirebaseUser

/**
 * Sealed class representing the different states of authentication operations.
 *
 * This provides a type-safe way to handle the various states that can occur
 * during authentication operations (loading, success, error, etc.).
 */
sealed class AuthState {
    /**
     * Base class for states where no authentication operation is in progress
     */
    sealed class Idle : AuthState() {
        /**
         * Initial state when the app starts and authentication status is unknown
         */
        data object Initial : Idle()

        /**
         * State when user is confirmed to be not authenticated
         */
        data object Unauthenticated : Idle()

        /**
         * State when user is authenticated with Firebase
         */
        data class Authenticated(val user: FirebaseUser) : Idle()

        /**
         * State when password reset email has been sent successfully
         */
        data object PasswordResetSent : Idle()
    }

    /**
     * Base class for states where an authentication operation is in progress
     */
    sealed class Loading : AuthState() {
        /**
         * Generic loading state for any authentication operation
         */
        data object InProgress : Loading()

        /**
         * Loading state specifically for login operation
         */
        data object Login : Loading()

        /**
         * Loading state specifically for registration operation
         */
        data object Registration : Loading()

        /**
         * Loading state specifically for password reset operation
         */
        data object PasswordReset : Loading()
    }

    /**
     * Base class for states where an authentication operation resulted in an error
     */
    sealed class Error : AuthState() {
        /**
         * Generic error state with a message
         */
        data class Generic(val message: String) : Error()

        /**
         * Network-related error state
         */
        data class Network(val message: String) : Error()

        /**
         * Authentication-specific error state (invalid credentials, etc.)
         */
        data class Authentication(val message: String) : Error()

        /**
         * Validation error state (invalid email format, password too short, etc.)
         */
        data class Validation(val message: String) : Error()
    }
}
