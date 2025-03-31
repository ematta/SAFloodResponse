package edu.utap.auth

import com.google.firebase.auth.FirebaseUser
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.db.UserEntity
import edu.utap.user.UserProfile
import edu.utap.user.repository.FirebaseUserRepository
import edu.utap.user.repository.UserRepository
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
     * @param firebaseUser The Firebase user to synchronize
     * @return Result containing the synchronized UserEntity
     */
    suspend fun syncUserToLocal(firebaseUser: FirebaseUser): Result<UserEntity> = try {
        // Get or create local user entity
        val localUserResult = authRepository.getLocalUserById(firebaseUser.uid)
        val userEntity = when {
            localUserResult.isSuccess -> localUserResult.getOrNull()
            else -> createNewUserEntity(firebaseUser)
        } ?: throw Exception("Failed to create or retrieve user entity")

        // Update Firestore profile
        val firestoreProfile = UserProfile(
            uid = userEntity.userId,
            displayName = userEntity.name,
            email = userEntity.email,
            photoUrl = userEntity.profilePic ?: ""
        )
        userRepository.updateUserProfile(firestoreProfile)

        Result.success(userEntity)
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }

    /**
     * Creates a new user entity from a Firebase user.
     * @param firebaseUser The Firebase user to create the entity from
     * @return The created UserEntity
     */
    private suspend fun createNewUserEntity(firebaseUser: FirebaseUser): UserEntity {
        val userEntity = UserEntity(
            userId = firebaseUser.uid,
            name = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: "",
            profilePic = firebaseUser.photoUrl?.toString(),
            role = "regular"
        )
        authRepository.createLocalUser(userEntity)
        return userEntity
    }

    /**
     * Updates a user's role and synchronizes the change.
     * @param userId The ID of the user to update
     * @param newRole The new role to set
     * @return Result containing the updated UserEntity
     */
    suspend fun updateUserRole(userId: String, newRole: String): Result<UserEntity> = try {
        val userResult = authRepository.getLocalUserById(userId)
        val userEntity = userResult.getOrNull() ?: throw Exception("User not found")

        val updatedUser = userEntity.copy(role = newRole)
        authRepository.updateLocalUser(updatedUser)

        // Update Firestore profile
        val firestoreProfile = UserProfile(
            uid = updatedUser.userId,
            displayName = updatedUser.name,
            email = updatedUser.email,
            photoUrl = updatedUser.profilePic ?: ""
        )
        userRepository.updateUserProfile(firestoreProfile)

        Result.success(updatedUser)
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }
}
