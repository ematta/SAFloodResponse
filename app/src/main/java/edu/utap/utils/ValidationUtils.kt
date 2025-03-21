package edu.utap.utils

import androidx.core.util.PatternsCompat.EMAIL_ADDRESS
import java.util.regex.Pattern

/**
 * Utility object for input validation functions.
 *
 * This object provides methods to validate user input such as email addresses,
 * passwords, and names. These validation functions are used throughout the
 * application to ensure data integrity before processing or storing user data.
 */
object ValidationUtils {
    /**
     * Gets the appropriate email pattern based on the environment.
     * In a production environment, it uses Android's Patterns.EMAIL_ADDRESS.
     * In a test environment, it uses the MockPatterns.EMAIL_ADDRESS.
     *
     * @return Pattern The email validation pattern to use
     */
    private fun getEmailPattern(): Pattern = EMAIL_ADDRESS

    /**
     * Validates an email address format.
     *
     * Uses Android's built-in pattern matcher to verify that the email
     * follows a standard email format (username@domain.tld).
     *
     * @param email The email address to validate
     * @return true if the email is valid, false otherwise
     */
    fun isValidEmail(email: String): Boolean =
        email.isNotEmpty() && getEmailPattern().matcher(email).matches()

    /**
     * Validates a password's strength.
     *
     * Currently only checks for minimum length of 6 characters.
     * Could be extended in the future to check for complexity requirements
     * such as special characters, numbers, or mixed case.
     *
     * @param password The password to validate
     * @return true if the password meets the minimum requirements, false otherwise
     */
    fun isValidPassword(password: String): Boolean = password.length >= 6

    /**
     * Validates an email address.
     *
     * This function checks if the provided email string is valid according to the following criteria:
     * 1. It's not empty.
     * 2. It matches the standard email address pattern.
     *
     * @param email The email address string to validate.
     * @return A Pair where:
     *         - The first element (Boolean) indicates whether the email is valid (true) or not (false).
     *         - The second element (String?) is a nullable error message.
     *           - If the email is valid, it's null.
     *           - If the email is invalid, it contains an error message explaining why.
     *
     * Example usage:
     * ```kotlin
     * val result1 = validateEmail("test@user.com") // result1.first == true, result1.second == null
     * val result2 = validateEmail("") // result2.first == false, result2.second == "Email cannot be empty"
     * val result3 = validateEmail("invalid-email") // result3.first == false, result3.second == "Please enter a valid email address"
     * ```
     */
    fun validateEmail(email: String): Pair<Boolean, String?> {
        if (email.isEmpty()) {
            return Pair(false, "Email cannot be empty")
        }
        if (!getEmailPattern().matcher(email).matches()) {
            return Pair(false, "Please enter a valid email address")
        }
        return Pair(true, null)
    }

    /**
     * Validates a password based on the following criteria:
     *   - Password cannot be empty.
     *   - Password must be at least 6 characters long.
     *
     * @param password The password string to validate.
     * @return A Pair containing:
     *   - A Boolean indicating whether the password is valid (true) or not (false).
     *   - A String? containing an error message if the password is invalid, or null if it is valid.
     */
    fun validatePassword(password: String): Pair<Boolean, String?> {
        if (password.isEmpty()) {
            return Pair(false, "Password cannot be empty")
        }
        if (password.length < 6) {
            return Pair(false, "Password must be at least 6 characters long")
        }
        return Pair(true, null)
    }

    /**
     * Validates the provided email and password for login.
     *
     * This function checks if the given email and password are valid based on
     * predefined validation rules (as implemented in `validateEmail` and `validatePassword`).
     * It returns a Pair indicating the overall validity of the credentials and an optional error message.
     *
     * @param email The email address to validate.
     * @param password The password to validate.
     * @return A Pair where:
     *         - first: A Boolean indicating whether the email and password are both valid (true) or not (false).
     *         - second: An optional String error message. Currently always null, as detailed validation errors
     *                   are handled within the `validateEmail` and `validatePassword` functions and not
     *                   propagated up here. If both are valid, this will be null.
     *                   If either is invalid this will also be null.
     *
     * @see validateEmail
     * @see validatePassword
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
