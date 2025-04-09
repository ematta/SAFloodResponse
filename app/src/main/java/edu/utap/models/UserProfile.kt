package edu.utap.models

/**
 * Data class representing a user profile in the application.
 *
 * This class stores comprehensive user profile information that extends beyond
 * the basic authentication data. It includes personal details and metadata
 * that are used throughout the application for user-related features.
 */
data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
