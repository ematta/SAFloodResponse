package edu.utap.user

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis()
) 