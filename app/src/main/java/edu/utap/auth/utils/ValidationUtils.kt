package edu.utap.auth.utils

import java.util.regex.Pattern

/**
 * Utility class for form validation
 */
object ValidationUtils {
    
    // Email regex pattern that works in both Android and unit tests
    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
        "@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(" +
        "\\." +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
        ")+"
    )
    
    /**
     * Validates an email address
     * @param email The email to validate
     * @return A pair containing a boolean indicating if the email is valid and an error message if invalid
     */
    fun validateEmail(email: String): Pair<Boolean, String?> {
        return when {
            email.isBlank() -> Pair(false, "Email cannot be empty")
            !EMAIL_PATTERN.matcher(email).matches() -> Pair(false, "Please enter a valid email address")
            else -> Pair(true, null)
        }
    }
    
    /**
     * Validates a password
     * @param password The password to validate
     * @return A pair containing a boolean indicating if the password is valid and an error message if invalid
     */
    fun validatePassword(password: String): Pair<Boolean, String?> {
        return when {
            password.isBlank() -> Pair(false, "Password cannot be empty")
            password.length < 6 -> Pair(false, "Password must be at least 6 characters long")
            else -> Pair(true, null)
        }
    }
    
    /**
     * Validates login credentials
     * @param email The email to validate
     * @param password The password to validate
     * @return A pair containing a boolean indicating if the credentials are valid and an error message if invalid
     */
    fun validateLoginCredentials(email: String, password: String): Pair<Boolean, String?> {
        val emailValidation = validateEmail(email)
        if (!emailValidation.first) {
            return emailValidation
        }
        
        val passwordValidation = validatePassword(password)
        if (!passwordValidation.first) {
            return passwordValidation
        }
        
        return Pair(true, null)
    }
}