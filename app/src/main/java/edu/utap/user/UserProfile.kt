package edu.utap.user

/**
 * Data class representing a user profile in the application.
 * 
 * This class stores comprehensive user profile information that extends beyond
 * the basic authentication data. It includes personal details and metadata
 * that are used throughout the application for user-related features.
 */
data class UserProfile(
    val uid: String = "",           // Unique identifier matching Firebase Auth UID
    val displayName: String = "",   // User's display name shown in the UI
    val email: String = "",         // User's email address for communication
    val photoUrl: String = "",      // URL to the user's profile picture
    val phoneNumber: String = "",   // User's contact phone number
    val address: String = "",       // User's physical address
    val createdAt: Long = System.currentTimeMillis() // Timestamp when profile was created
)