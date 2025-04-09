package edu.utap.auth

import com.google.firebase.auth.FirebaseUser
import edu.utap.repository.AuthRepositoryInterface
// Removed import edu.utap.db.UserEntity
import edu.utap.user.UserProfile
import edu.utap.repository.FirebaseUserRepository
import edu.utap.repository.UserRepository
import edu.utap.utils.FirebaseErrorMapper

/**
 * Handles synchronization of user data between Firebase and local storage.
 * This class ensures data consistency across different storage locations.
 */
class UserDataSynchronizer(
    private val authRepository: AuthRepositoryInterface,
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

        Result.success(firebaseUser) // Return the original FirebaseUser
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }

    // Removed createNewUserEntity function as UserEntity is removed
    /**
     * Updates a user's role and synchronizes the change.
     * @param userId The ID of the user to update
     * @param newRole The new role to set (Note: This function needs complete redesign)
     * @return Result containing the FirebaseUser if successful (Not implemented)
     */
    suspend fun updateUserRole(userId: String, newRole: String): Result<FirebaseUser> = try {
        // TODO: Implement role update logic.
        // This requires updating custom claims or Firestore directly.
        // The previous logic relied on a local UserEntity which is now removed.
        // Cannot return a FirebaseUser object representing the update easily.
        throw UnsupportedOperationException("updateUserRole needs redesign after UserEntity removal")
        // Result.success(updatedUser) // Placeholder removed
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }
}
