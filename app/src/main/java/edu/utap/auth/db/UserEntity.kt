package edu.utap.auth.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a user in the database.
 * 
 * This entity stores user profile information including authentication details,
 * personal information, and role-based access control data. It serves as the
 * primary user record in the local database and corresponds to Firebase Auth users.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,     // Unique identifier for the user, matches Firebase Auth UID
    val name: String,       // User's display name
    val email: String,      // User's email address used for authentication
    val profilePic: String? = null, // Optional URL to user's profile picture
    val role: String,       // Access control role: 'regular', 'volunteer', or 'admin'
    val city: String? = null,    // Optional user's city for location-based features
    val county: String? = null   // Optional user's county for location-based features
)