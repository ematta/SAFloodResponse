package edu.utap.models

/**
 * Data class representing a user profile in the application.
 *
 * This class stores comprehensive user profile information that extends beyond
 * the basic authentication data. It includes personal details and metadata
 * that are used throughout the application for user-related features.
 *
 * @property uid Unique identifier for the user.
 * @property displayName Display name of the user.
 * @property email Email address of the user.
 * @property photoUrl URL of the user's profile picture.
 * @property phoneNumber Phone number of the user.
 * @property address Address of the user.
 * @property createdAt Timestamp when the user was created.
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
