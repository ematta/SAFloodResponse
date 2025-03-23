package edu.utap.auth.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * Utility class to map Firebase authentication exceptions to user-friendly error messages
 */
object FirebaseErrorMapper {
    
    /**
     * Maps a Firebase exception to a user-friendly error message
     * @param exception The exception to map
     * @return A user-friendly error message
     */
    fun getErrorMessage(exception: Throwable): String {
        return when (exception) {
            // Network related errors
            is FirebaseNetworkException -> "Unable to connect to the server. Please check your internet connection and try again."
            
            // Invalid credentials
            is FirebaseAuthInvalidCredentialsException -> {
                when (exception.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
                    "ERROR_WRONG_PASSWORD" -> "The password is incorrect. Please try again."
                    "ERROR_INVALID_CREDENTIAL" -> "The login credentials are invalid. Please try again."
                    else -> "Invalid login credentials. Please check your email and password."
                }
            }
            
            // User does not exist or has been disabled
            is FirebaseAuthInvalidUserException -> {
                when (exception.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> "There is no account with this email. Please check your email or create a new account."
                    "ERROR_USER_DISABLED" -> "This account has been disabled. Please contact support for assistance."
                    "ERROR_USER_TOKEN_EXPIRED" -> "Your session has expired. Please sign in again."
                    else -> "Account error. Please try again or contact support."
                }
            }
            
            // Email already in use
            is FirebaseAuthUserCollisionException -> {
                when (exception.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists. Please use a different email or try logging in."
                    "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "An account already exists with the same email but different sign-in credentials."
                    else -> "This email is already associated with an account."
                }
            }
            
            // Weak password
            is FirebaseAuthWeakPasswordException -> "The password is too weak. Please use a stronger password with at least 6 characters."
            
            // General Firebase auth errors
            is FirebaseAuthException -> {
                when (exception.errorCode) {
                    "ERROR_OPERATION_NOT_ALLOWED" -> "This operation is not allowed. Please contact support."
                    "ERROR_TOO_MANY_REQUESTS" -> "Too many unsuccessful attempts. Please try again later or reset your password."
                    "ERROR_REQUIRES_RECENT_LOGIN" -> "This operation requires recent authentication. Please log in again before retrying."
                    else -> "Authentication error: ${exception.message}"
                }
            }
            
            // Default case for other exceptions
            else -> exception.message ?: "An unexpected error occurred. Please try again."
        }
    }
}