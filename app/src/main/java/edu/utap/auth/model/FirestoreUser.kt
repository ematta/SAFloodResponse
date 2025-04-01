package edu.utap.auth.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

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
