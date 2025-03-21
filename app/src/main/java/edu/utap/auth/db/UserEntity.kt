package edu.utap.auth.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val name: String,
    val email: String,
    val profilePic: String? = null,
    val role: String, // 'regular', 'volunteer', or 'admin'
    val city: String? = null,
    val county: String? = null
) 