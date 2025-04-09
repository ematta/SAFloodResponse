package edu.utap.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.models.UserProfile
import kotlinx.coroutines.tasks.await

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
    suspend fun createUserProfile(userProfile: UserProfile): Result<UserProfile>

    /**
     * Retrieves a user profile by user ID.
     *
     * @param uid The unique identifier of the user
     * @return Result containing the user profile or an error if not found
     */
    suspend fun getUserProfile(uid: String): Result<UserProfile>

    /**
     * Updates an existing user profile with new data.
     *
     * @param userProfile The updated user profile data
     * @return Result containing the updated profile or an error
     */
    suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile>

    /**
     * Updates only the display name of a user.
     *
     * @param uid The unique identifier of the user
     * @param displayName The new display name
     * @return Result indicating success or failure
     */
    suspend fun updateDisplayName(uid: String, displayName: String): Result<Unit>

    /**
     * Updates only the photo URL of a user.
     *
     * @param uid The unique identifier of the user
     * @param photoUrl The new photo URL
     * @return Result indicating success or failure
     */
    suspend fun updatePhotoUrl(uid: String, photoUrl: String): Result<Unit>
}

/**
 * Firebase implementation of the UserRepository interface.
 *
 * This class handles user profile operations using Firebase Authentication
 * and Firestore for persistent storage. It synchronizes data between
 * Firebase Auth user profiles and the Firestore database.
 */
class FirebaseUserRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    /**
     * Creates a new user profile in Firestore and updates Firebase Auth profile.
     *
     * This method performs a multi-step process:
     * 1. Stores the complete profile in Firestore
     * 2. Updates the Firebase Auth user profile with display name if provided
     * 3. Updates the Firebase Auth user profile with photo URL if provided
     *
     * @param userProfile The user profile to create
     * @return Result containing the created profile or an error
     */
    override suspend fun createUserProfile(userProfile: UserProfile): Result<UserProfile> = try {
        // Store the user profile in Firestore
        usersCollection.document(userProfile.uid)
            .set(userProfile)
            .await()

        // Update display name in Firebase Auth if provided
        if (userProfile.displayName.isNotEmpty()) {
            updateDisplayName(userProfile.uid, userProfile.displayName)
        }

        // Update photo URL in Firebase Auth if provided
        if (userProfile.photoUrl.isNotEmpty()) {
            updatePhotoUrl(userProfile.uid, userProfile.photoUrl)
        }

        Result.success(userProfile)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Retrieves a user profile from Firestore by user ID.
     *
     * This method queries the Firestore database for a user document with the specified ID.
     * If found, it converts the document to a UserProfile object.
     *
     * @param uid The unique identifier of the user to retrieve
     * @return Result containing the user profile or an error if not found or parsing fails
     */
    override suspend fun getUserProfile(uid: String): Result<UserProfile> {
        var status: Result<UserProfile> = Result.failure(Exception("User profile not found"))
        val document = usersCollection.document(uid).get().await()
        if (document != null && document.exists()) {
            // Manually construct UserProfile from document data to ensure proper field mapping
            val data = document.data
            if (data != null) {
                val userProfile = UserProfile(
                    uid = data["uid"] as? String ?: "",
                    displayName = data["displayName"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    photoUrl = data["photoUrl"] as? String ?: "",
                    phoneNumber = data["phoneNumber"] as? String ?: "",
                    address = data["address"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                )
                status = Result.success(userProfile)
            } else {
                status =
                    Result.failure(Exception("Failed to parse user profile: document data is null"))
            }
        }
        return status
    }

    /**
     * Updates an existing user profile in Firestore and Firebase Auth.
     *
     * This method performs a multi-step update process:
     * 1. Updates the complete profile in Firestore
     * 2. Updates the Firebase Auth user profile with display name if provided
     * 3. Updates the Firebase Auth user profile with photo URL if provided
     *
     * @param userProfile The updated user profile data
     * @return Result containing the updated profile or an error
     */
    override suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile> = try {
        // Update the user profile in Firestore
        usersCollection.document(userProfile.uid)
            .set(userProfile)
            .await()

        // Update display name in Firebase Auth if provided
        if (userProfile.displayName.isNotEmpty()) {
            updateDisplayName(userProfile.uid, userProfile.displayName)
        }

        // Update photo URL in Firebase Auth if provided
        if (userProfile.photoUrl.isNotEmpty()) {
            updatePhotoUrl(userProfile.uid, userProfile.photoUrl)
        }

        Result.success(userProfile)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Updates only the display name in Firebase Auth.
     *
     * This method updates the display name in the Firebase Auth user profile.
     * It verifies that the current authenticated user matches the requested user ID
     * to prevent unauthorized profile modifications.
     *
     * @param uid The unique identifier of the user
     * @param displayName The new display name
     * @return Result indicating success or failure
     */
    override suspend fun updateDisplayName(uid: String, displayName: String): Result<Unit> = try {
        val currentUser = firebaseAuth.currentUser
        if (currentUser?.uid == uid) {
            val profileUpdates = userProfileChangeRequest {
                this.displayName = displayName
            }
            currentUser.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } else {
            Result.failure(Exception("Cannot update display name for another user"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Updates only the photo URL in Firebase Auth.
     *
     * This method updates the photo URL in the Firebase Auth user profile.
     * It verifies that the current authenticated user matches the requested user ID
     * to prevent unauthorized profile modifications.
     *
     * @param uid The unique identifier of the user
     * @param photoUrl The new photo URL
     * @return Result indicating success or failure
     */
    override suspend fun updatePhotoUrl(uid: String, photoUrl: String): Result<Unit> = try {
        val currentUser = firebaseAuth.currentUser
        if (currentUser?.uid == uid) {
            val profileUpdates = userProfileChangeRequest {
                this.photoUri = Uri.parse(photoUrl)
            }
            currentUser.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } else {
            Result.failure(Exception("Cannot update photo URL for another user"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
