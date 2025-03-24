package edu.utap.auth.utils

import java.util.regex.Pattern

/**
 * Mock implementation of Android's Patterns class for unit tests.
 * 
 * This class provides a mock implementation of the Android Patterns.EMAIL_ADDRESS
 * static field that is used in ValidationUtils. In unit tests, the Android framework
 * classes are not available, which causes NullPointerExceptions when trying to use
 * android.util.Patterns.EMAIL_ADDRESS.
 */
object MockPatterns {
    /**
     * A simplified email pattern for validation in tests.
     * This pattern matches strings that contain an @ symbol with text before and after it.
     */
    val EMAIL_ADDRESS: Pattern = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    )
}