package edu.utap.auth.repository

import com.google.firebase.auth.FirebaseUser
import edu.utap.auth.db.UserEntity
import edu.utap.auth.utils.RoleUtils

/**
 * Extension function to create a UserEntity from a FirebaseUser
 */
fun FirebaseUser.toUserEntity(role: String = RoleUtils.ROLE_REGULAR): UserEntity {
    return UserEntity(
        userId = this.uid,
        name = this.displayName ?: "",
        email = this.email ?: "",
        profilePic = this.photoUrl?.toString(),
        role = role
    )
}

/**
 * Extension function to update an existing UserEntity with Firebase user data
 */
fun UserEntity.updateFromFirebaseUser(user: FirebaseUser): UserEntity {
    return this.copy(
        name = user.displayName ?: this.name,
        email = user.email ?: this.email,
        profilePic = user.photoUrl?.toString() ?: this.profilePic
    )
} 