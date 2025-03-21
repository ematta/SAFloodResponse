package edu.utap.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.google.firebase.auth.MultiFactorResolver
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseErrorMapperTest {

    @Test
    fun `getErrorMessage Network Exception`() {
        val exception = FirebaseNetworkException("Network error")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("Connection failed. Please check your internet and try again.", message)
    }

    @Test
    fun `getErrorMessage Multi Factor Exception`() {
        val resolver = mockk<MultiFactorResolver>()
        val exception = FirebaseAuthMultiFactorException("multi-factor", "Multi factor required", resolver)
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("Additional verification required. Please check your authentication app.", message)
    }

    @Test
    fun `getErrorMessage Recent Login Required`() {
        val exception = FirebaseAuthRecentLoginRequiredException("recent-login", "Recent login required")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("Recent login required for security. Please sign in again.", message)
    }

    @Test
    fun `getErrorMessage Invalid Email`() {
        val exception = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "Invalid email")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("The email address is badly formatted.", message)
    }

    @Test
    fun `getErrorMessage Wrong Password`() {
        val exception = FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "Wrong password")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("The password is incorrect. Please try again.", message)
    }

    @Test
    fun `getErrorMessage Invalid Credential`() {
        val exception = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "Invalid credential")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("The login credentials are invalid. Please try again.", message)
    }

    @Test
    fun `getErrorMessage Generic Invalid Credential`() {
        val exception = FirebaseAuthInvalidCredentialsException("UNKNOWN_CODE", "Unknown error")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("Invalid login credentials. Please check your email and password.", message)
    }

    @Test
    fun `getErrorMessage User Not Found`() {
        val exception = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "User not found")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("There is no account with this email. Please check your email or create a new account.", message)
    }

    @Test
    fun `getErrorMessage User Disabled`() {
        val exception = FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "User disabled")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("This account has been disabled. Please contact support for assistance.", message)
    }

    @Test
    fun `getErrorMessage User Token Expired`() {
        val exception = FirebaseAuthInvalidUserException("ERROR_USER_TOKEN_EXPIRED", "Token expired")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("Your session has expired. Please sign in again.", message)
    }

    @Test
    fun `getErrorMessage Generic Account Error`() {
        val exception = FirebaseAuthInvalidUserException("UNKNOWN_CODE", "Unknown error")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("Account error. Please try again or contact support.", message)
    }

    @Test
    fun `getErrorMessage Email Already In Use`() {
        val exception = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "Email already in use")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("An account with this email already exists. Please use a different email or try logging in.", message)
    }

    @Test
    fun `getErrorMessage Account Exists With Different Credential`() {
        val exception = FirebaseAuthUserCollisionException("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "Account exists with different credential")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("An account already exists with the same email but different sign-in credentials.", message)
    }

    @Test
    fun `getErrorMessage Generic Email Collision`() {
        val exception = FirebaseAuthUserCollisionException("UNKNOWN_CODE", "Unknown error")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("This email is already associated with an account.", message)
    }

    // Skipping tests for FirebaseAuthException error codes without provided mappings (lines 102-177)

    @Test
    fun `getErrorMessage Other Exception`() {
        val exception = IllegalArgumentException("Some other error")
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("Some other error", message)
    }

    @Test
    fun `getErrorMessage Null Exception Message`() {
        val exception = Exception(null as String?)
        val message = FirebaseErrorMapper.getErrorMessage(exception)
        assertEquals("An unexpected error occurred. Please try again.", message)
    }

    @Test
    fun `mapFirebaseAuthError Invalid User`() {
        val exception = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "User not found")
        val message = FirebaseErrorMapper.mapFirebaseAuthError(exception)
        assertEquals("User not found. Please check your email or register.", message)
    }

    @Test
    fun `mapFirebaseAuthError Invalid Credentials`() {
        val exception = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "Invalid credentials")
        val message = FirebaseErrorMapper.mapFirebaseAuthError(exception)
        assertEquals("Invalid credentials. Please check your email and password.", message)
    }

    @Test
    fun `mapFirebaseAuthError User Collision`() {
        val exception = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "User collision")
        val message = FirebaseErrorMapper.mapFirebaseAuthError(exception)
        assertEquals("An account already exists with this email address.", message)
    }

//    @Test
//    fun `mapFirebaseAuthError Weak Password`() {
//        val exception = FirebaseAuthWeakPasswordException("weak-password", "Weak password", null)
//        val message = FirebaseErrorMapper.mapFirebaseAuthError(exception)
//        assertEquals("The password is too weak. Please use a stronger password with at least 6 characters.", message)
//    }

    @Test
    fun `mapFirebaseAuthError Network Exception`() {
        val exception = FirebaseNetworkException("Network error")
        val message = FirebaseErrorMapper.mapFirebaseAuthError(exception)
        assertEquals("Network error. Please check your internet connection.", message)
    }

    @Test
    fun `mapFirebaseAuthError Other Exception`() {
        val exception = Exception("Some auth failure")
        val message = FirebaseErrorMapper.mapFirebaseAuthError(exception)
        assertEquals("Authentication failed: Some auth failure", message)
    }

    @Test
    fun `mapFirebaseAuthError Null Exception Message`() {
        val exception = Exception(null as String?)
        val message = FirebaseErrorMapper.mapFirebaseAuthError(exception)
        assertEquals("Authentication failed: null", message)
    }
}
