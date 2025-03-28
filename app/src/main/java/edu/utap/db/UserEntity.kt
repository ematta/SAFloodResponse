package edu.utap.db

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
    val userId: String,
    val name: String,
    val email: String,
    val profilePic: String? = null,
    val role: String,
    val city: String? = null,
    val county: String? = null
)
