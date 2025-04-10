package edu.utap.auth

import com.google.firebase.auth.FirebaseUser
import edu.utap.models.UserProfile
import edu.utap.repository.FirebaseUserRepository
import edu.utap.repository.UserRepository
import edu.utap.utils.FirebaseErrorMapper

/**
 * Handles synchronization of user data between Firebase and local storage.
 * This class ensures data consistency across different storage locations.
 */
class UserDataSynchronizer(
    private val userRepository: UserRepository = FirebaseUserRepository()
) {
    /**
     * Synchronizes a Firebase user with local storage.
     * @param firebaseUser The Firebase user whose profile should be synced to Firestore
     * @return Result containing the FirebaseUser if successful
     */
    suspend fun syncUserToLocal(firebaseUser: FirebaseUser): Result<FirebaseUser> = try {
        // Removed logic related to local UserEntity fetching/creation

        // Update Firestore profile
        val firestoreProfile = UserProfile(
            uid = firebaseUser.uid,
            displayName = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: "",
            photoUrl = firebaseUser.photoUrl?.toString() ?: ""
        )
        userRepository.updateUserProfile(firestoreProfile)

        Result.success(firebaseUser)
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }
}
