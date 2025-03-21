package edu.utap.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.models.UserProfile
import kotlinx.coroutines.tasks.await
import kotlin.Result

interface UserRepository {
    suspend fun createUserProfile(userProfile: UserProfile): Result<UserProfile>
    // Returns the user profile directly
    suspend fun getUserProfile(uid: String): Result<UserProfile>
    suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile>
    suspend fun updateDisplayName(uid: String, displayName: String): Result<Unit>
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
) : BaseRepository(),
    UserRepository {

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
    override suspend fun createUserProfile(userProfile: UserProfile): Result<UserProfile> {
        usersCollection.document(userProfile.uid)
            .set(userProfile)
            .await()

        if (userProfile.displayName.isNotEmpty()) {
            updateDisplayName(userProfile.uid, userProfile.displayName)
        }

        if (userProfile.photoUrl.isNotEmpty()) {
            updatePhotoUrl(userProfile.uid, userProfile.photoUrl)
        }

        return Result.success(userProfile)
    }

    /**
     * Retrieves a user profile from Firestore by user ID.
     *
     * @param uid The unique identifier of the user to retrieve
     * @return Result containing the user profile or an error if not found or parsing fails
     */
    override suspend fun getUserProfile(uid: String): Result<UserProfile> {
        val document = usersCollection.document(uid).get().await()
        if (!document.exists()) {
            return Result.failure(Exception("User profile not found"))
        }
        val profile = document.toDomainObject<UserProfile>()
            ?: return Result.failure(Exception("Failed to parse user profile"))
        return Result.success(profile)
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
    override suspend fun updateUserProfile(
        userProfile: UserProfile
    ): Result<UserProfile> {
        usersCollection.document(userProfile.uid)
            .set(userProfile)
            .await()

        if (userProfile.displayName.isNotEmpty()) {
            updateDisplayName(userProfile.uid, userProfile.displayName)
        }

        if (userProfile.photoUrl.isNotEmpty()) {
            updatePhotoUrl(userProfile.uid, userProfile.photoUrl)
        }

        return Result.success(userProfile)
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
    override suspend fun updateDisplayName(
        uid: String,
        displayName: String
    ): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
        if (currentUser?.uid == uid) {
            val profileUpdates = userProfileChangeRequest {
                this.displayName = displayName
            }
            currentUser.updateProfile(profileUpdates).await()
        } else {
            return Result.failure(Exception("Cannot update display name for another user"))
        }
        return Result.success(Unit)
    }

    /**
     * Updates only the photo URL in Firebase Auth and Firestore.
     *
     * This method updates the photo URL in the Firebase Auth user profile and
     * synchronizes the change with the Firestore user document.
     * It verifies that the current authenticated user matches the requested user ID
     * to prevent unauthorized profile modifications.
     *
     * @param uid The unique identifier of the user
     * @param photoUrl The new photo URL
     * @return Result indicating success or failure
     */
    override suspend fun updatePhotoUrl(
        uid: String,
        photoUrl: String
    ): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
        if (currentUser?.uid == uid) {
            // Update Firebase Auth profile photo
            val profileUpdates = userProfileChangeRequest {
                this.photoUri = Uri.parse(photoUrl)
            }
            currentUser.updateProfile(profileUpdates).await()
            // Update Firestore user document
            usersCollection.document(uid)
                .update("photoUrl", photoUrl)
                .await()
            return Result.success(Unit)
        } else {
            return Result.failure(Exception("Cannot update photo URL for another user"))
        }
    }
}


