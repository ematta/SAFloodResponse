package edu.utap.repository

import edu.utap.models.UserProfile

/**
 * Interface defining operations for managing user profiles.
 *
 * This interface abstracts the user profile management operations,
 * allowing for different implementations (e.g., Firebase, local database)
 * and facilitating testing through dependency injection.
 */
interface UserRepository {
    /**
     * Creates a new user profile in the storage system.
     *
     * @param userProfile The user profile data to store
     * @return Result containing the created profile or an error
     */
    suspend fun createUserProfile(userProfile: UserProfile): edu.utap.utils.Result<UserProfile>

    /**
     * Retrieves a user profile by user ID.
     *
     * @param uid The unique identifier of the user
     * @return Result containing the user profile or an error if not found
     */
    suspend fun getUserProfile(uid: String): edu.utap.utils.Result<UserProfile>

    /**
     * Updates an existing user profile with new data.
     *
     * @param userProfile The updated user profile data
     * @return Result containing the updated profile or an error
     */
    suspend fun updateUserProfile(userProfile: UserProfile): edu.utap.utils.Result<UserProfile>

    /**
     * Updates only the display name of a user.
     *
     * @param uid The unique identifier of the user
     * @param displayName The new display name
     * @return Result indicating success or failure
     */
    suspend fun updateDisplayName(uid: String, displayName: String): edu.utap.utils.Result<Unit>

    /**
     * Updates only the photo URL of a user.
     *
     * @param uid The unique identifier of the user
     * @param photoUrl The new photo URL
     * @return Result indicating success or failure
     */
    suspend fun updatePhotoUrl(uid: String, photoUrl: String): edu.utap.utils.Result<Unit>
}
