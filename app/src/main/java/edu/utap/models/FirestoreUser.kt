package edu.utap.models

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a user profile stored in Firestore.
 *
 * Includes user ID, name, email, profile picture URL, role, location info,
 * and timestamps for creation and update.
 *
 * @property userId Unique identifier for the user.
 * @property name Display name of the user.
 * @property email Email address of the user.
 * @property profilePic URL of the user's profile picture.
 * @property role Role of the user (e.g., "user", "admin").
 * @property city City where the user resides.
 * @property county County where the user resides.
 * @property createdAt Timestamp when the user was created.
 * @property updatedAt Timestamp when the user was last updated.
 */
data class FirestoreUser(
    @DocumentId
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    @get:PropertyName("profile_pic")
    @set:PropertyName("profile_pic")
    var profilePic: String? = null,
    val role: String = "user",
    val city: String? = null,
    val county: String? = null,
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Creates a [FirestoreUser] instance from a Firebase Authentication user.
         *
         * @param firebaseUser The Firebase Authentication user.
         * @return A corresponding [FirestoreUser].
         */
        fun fromFirebaseUser(firebaseUser: FirebaseUser): FirestoreUser = FirestoreUser(
            userId = firebaseUser.uid,
            name = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: "",
            profilePic = firebaseUser.photoUrl?.toString()
        )
    }
}
