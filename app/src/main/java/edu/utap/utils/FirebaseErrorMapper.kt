package edu.utap.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * Utility object for mapping Firebase-specific exceptions to user-friendly error messages.
 *
 * This mapper converts technical Firebase error messages into clear, actionable messages
 * that can be displayed to users. It handles common authentication errors such as:
 * - Invalid credentials
 * - User not found
 * - Email already in use
 * - Weak password
 * - Network issues
 */
@Suppress("ktlint:standard:max-line-length")
object FirebaseErrorMapper {

    /**
     * Converts a Firebase exception into a user-friendly error message.
     *
     * This method examines the type of exception and returns an appropriate
     * error message that explains the issue in non-technical terms and
     * provides guidance on how to resolve it.
     *
     * @param exception The exception thrown by Firebase
     * @return A user-friendly error message
     */
    fun getErrorMessage(exception: Throwable): String = when (exception) {
        // Network related errors
        is FirebaseNetworkException -> "Connection failed. Please check your internet and try again."

        // Multi-factor auth errors
        is FirebaseAuthMultiFactorException -> "Additional verification required. Please check your authentication app."
        is FirebaseAuthRecentLoginRequiredException -> "Recent login required for security. Please sign in again."

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
                "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Account temporarily locked. Try again in 5 minutes or reset password."
                "ERROR_INVALID_VERIFICATION_CODE" -> "Invalid verification code. Please check the code sent to your email/phone."
                "ERROR_MISSING_VERIFICATION_CODE" -> "Missing verification code. Please enter the code from your authentication app."
                "ERROR_SECOND_FACTOR_REQUIRED" -> "Two-factor authentication required. Please check your second factor method."
                "invalid-phone-number" -> "Invalid phone number format. Please use international format (+1xxx...)"
                "requires-recent-login" -> "Security check required. Please sign in again to complete this action."
                "unverified-email" -> "Email verification required. Please check your inbox and verify your email."
                "quota-exceeded" -> "Account quota exceeded. Please contact support for assistance."
                else -> "Authentication error: ${exception.message}"
            }
        }

        // Default case for other exceptions
        else -> exception.message ?: "An unexpected error occurred. Please try again."
    }

    /**
     * Maps a Firebase authentication exception to a user-friendly error message.
     *
     * This method examines the type of exception thrown by Firebase Authentication
     * and returns an appropriate message that explains the issue in terms that
     * non-technical users can understand and potentially resolve.
     *
     * @param exception The Firebase exception to map
     * @return A user-friendly error message explaining the authentication failure
     */
    fun mapFirebaseAuthError(exception: Exception): String = when (exception) {
        // User doesn't exist in Firebase Authentication
        is FirebaseAuthInvalidUserException -> "User not found. Please check your email or register."

        // Invalid login credentials (wrong email/password combination)
        is FirebaseAuthInvalidCredentialsException -> "Invalid credentials. Please check your email and password."

        // Email already in use by another account
        is FirebaseAuthUserCollisionException -> "An account already exists with this email address."

        // Password doesn't meet Firebase's strength requirements
        is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use a stronger password."

        // Network connectivity issues
        is FirebaseNetworkException -> "Network error. Please check your internet connection."

        // Fallback for any other unexpected authentication errors
        else -> "Authentication failed: ${exception.message}"
    }
}
