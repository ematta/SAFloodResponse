package edu.utap.auth.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a user profile stored in Firestore.
 *
 * Includes user ID, name, email, profile picture URL, role, location info,
 * and timestamps for creation and update.
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
        fun fromFirebaseUser(firebaseUser: com.google.firebase.auth.FirebaseUser): FirestoreUser {
            return FirestoreUser(
                userId = firebaseUser.uid,
                name = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: "",
                profilePic = firebaseUser.photoUrl?.toString()
            )
        }
    }
}
