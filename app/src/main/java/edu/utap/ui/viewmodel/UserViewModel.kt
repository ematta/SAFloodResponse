package edu.utap.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.utap.user.FirebaseStorageUtil
import edu.utap.user.FirebaseStorageUtilInterface
import edu.utap.user.UserProfile
import edu.utap.user.UserProfileState
import edu.utap.repository.FirebaseUserRepository
import edu.utap.repository.UserRepository
import edu.utap.utils.DefaultApplicationContextProvider
import edu.utap.utils.DefaultStorageUtilProvider
import edu.utap.utils.NetworkUtilsInterface
import edu.utap.utils.NetworkUtilsProvider
import edu.utap.utils.StorageUtilProviderInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Sealed class representing the different states of user profile operations.
 *
 * This provides a type-safe way to handle the various states that can occur
 * during profile operations (loading, success, error, etc.).
 */

/**
 * ViewModel responsible for managing user profile data and operations.
 *
 * This ViewModel handles:
 * - Creating user profiles
 * - Retrieving user profiles
 * - Updating user profiles
 * - Managing profile pictures
 *
 * It includes network connectivity checks before attempting operations
 * that require internet access, and exposes profile state through a StateFlow.
 */
class UserViewModel(
    private val userRepository: UserRepository = FirebaseUserRepository(),
) : ViewModel() {

    /**
     * StateFlow to expose the current profile operation state to UI components.
     * Initially set to Idle state when no operations are in progress.
     */
    private val _profileState =
        MutableStateFlow<UserProfileState>(value = UserProfileState.Idle.Initial)
    val profileState: StateFlow<UserProfileState> = _profileState

    /**
     * Creates a new user profile in Firestore.
     *
     * This method performs the following steps:
     * 1. Updates the profile state to Loading
     * 2. Checks for network connectivity
     * 3. Calls the repository to create the profile in Firestore
     * 4. Updates the profile state based on the result
     *
     * Network connectivity is checked before attempting to create the profile to provide
     * immediate feedback to the user if they're offline.
     *
     * @param userProfile The UserProfile object containing the user's profile information
     */
    fun createUserProfile(userProfile: UserProfile) {
        // Update state to indicate loading
        _profileState.value = UserProfileState.Loading.Creating

        viewModelScope.launch {
            val result = userRepository.createUserProfile(userProfile)
            result.fold(
                onSuccess = { profile ->
                    _profileState.value = UserProfileState.Success(profile)
                },
                onFailure = { error ->
                    _profileState.value =
                        UserProfileState.Error.Generic(
                            error.message ?: "Failed to create user profile"
                        )
                }
            )
        }
    }

    /**
     * Retrieves a user profile from Firestore by user ID.
     *
     * This method performs the following steps:
     * 1. Updates the profile state to Loading
     * 2. Checks for network connectivity
     * 3. Calls the repository to fetch the profile from Firestore
     * 4. Updates the profile state based on the result
     *
     * This method is typically called when a user logs in or when profile
     * information needs to be displayed in the UI.
     *
     * @param uid The unique identifier of the user whose profile to retrieve
     */
    fun getUserProfile(uid: String) {
        // Update state to indicate loading
        _profileState.value = UserProfileState.Loading.Fetching

        viewModelScope.launch {
            val result = userRepository.getUserProfile(uid)
            result.fold(
                onSuccess = { profile ->
                    _profileState.value = UserProfileState.Success(profile)
                },
                onFailure = { error ->
                    _profileState.value =
                        UserProfileState.Error.Generic(
                            error.message ?: "Failed to get user profile"
                        )
                }
            )
        }
    }

    /**
     * Updates an existing user profile in Firestore.
     *
     * This method performs the following steps:
     * 1. Updates the profile state to Loading
     * 2. Checks for network connectivity
     * 3. Calls the repository to update the profile in Firestore
     * 4. Updates the profile state based on the result
     *
     * This method is typically called when a user edits their profile information
     * through the profile screen.
     *
     * @param userProfile The UserProfile object containing the updated profile information
     */
    fun updateUserProfile(userProfile: UserProfile) {
        // Update state to indicate loading
        _profileState.value = UserProfileState.Loading.Updating

        viewModelScope.launch {
            val result = userRepository.updateUserProfile(userProfile)
            result.fold(
                onSuccess = { profile ->
                    _profileState.value = UserProfileState.Success(profile)
                },
                onFailure = { error ->
                    _profileState.value =
                        UserProfileState.Error.Generic(
                            error.message ?: "Failed to update user profile"
                        )
                }
            )
        }
    }

    /**
     * Updates a user's display name in Firestore.
     *
     * This method performs the following steps:
     * 1. Updates the profile state to Loading
     * 2. ~Checks for network connectivity~
     * 3. Calls the repository to update the display name in Firestore
     * 4. Either refreshes the full profile or creates a minimal success state
     *    depending on network connectivity
     *
     * @param uid The unique identifier of the user whose display name to update
     * @param displayName The new display name to set for the user
     */
    fun updateDisplayName(uid: String, displayName: String) {
        // Update state to indicate loading
        _profileState.value = UserProfileState.Loading.Updating

        viewModelScope.launch {
            val result = userRepository.updateDisplayName(uid, displayName)
            result.fold(
                onSuccess = { getUserProfile(uid) },
                onFailure = { error ->
                    // Display name update failed, provide error message
                    _profileState.value =
                        UserProfileState.Error.Generic(
                            error.message ?: "Failed to update display name"
                        )
                }
            )
        }
    }

    /**
     * Updates a user's profile photo URL in Firestore.
     *
     * This method performs the following steps:
     * 1. Updates the profile state to Loading
     * 2. Checks for network connectivity
     * 3. Calls the repository to update the photo URL in Firestore
     * 4. Either refreshes the full profile or creates a minimal success state
     *    depending on network connectivity
     *
     * This method includes special handling for the success case:
     * - If network is available after the update, it refreshes the full profile
     * - If network becomes unavailable, it creates a minimal success state with just the updated photo URL
     *
     * @param uid The unique identifier of the user whose photo URL to update
     * @param photoUrl The new photo URL to set for the user's profile
     */
    fun updatePhotoUrl(uid: String, photoUrl: String) {
        // Update state to indicate loading
        _profileState.value = UserProfileState.Loading.Updating
        viewModelScope.launch {
            val result = userRepository.updatePhotoUrl(uid, photoUrl)
            result.fold(
                onSuccess = { getUserProfile(uid) },
                onFailure = { error ->
                    // Photo URL update failed, provide error message
                    _profileState.value =
                        UserProfileState.Error.Generic(
                            error.message ?: "Failed to update photo URL"
                        )
                }
            )
        }
    }

    /**
     * Uploads a profile image to Firebase Storage and updates the user's profile.
     *
     * This method performs a two-step process:
     * 1. Uploads the image to Firebase Storage
     * 2. Updates the user's profile with the download URL of the uploaded image
     *
     * The method includes network connectivity checks and proper error handling
     * for both the upload and profile update operations.
     *
     * @param uploadContext The Android Context needed for file operations
     * @param imageUri The URI of the image to upload
     * @param uid The unique identifier of the user whose profile image to update
     */
    fun uploadProfileImage(uploadContext: Context, imageUri: Uri, uid: String) {
        // Update state to indicate loading
        _profileState.value = UserProfileState.Loading.Creating
        viewModelScope.launch {
            // Step 1: Upload the image to Firebase Storage
            val storageUtil = FirebaseStorageUtil()
            val uploadResult = storageUtil.uploadProfileImage(
                context = uploadContext,
                imageUri = imageUri,
                userId = uid
            )
            uploadResult.fold(
                onSuccess = { downloadUrl ->
                    // Step 2: Update the photo URL in the user profile
                    // This will trigger another network operation to update the profile
                    updatePhotoUrl(uid, downloadUrl)
                },
                onFailure = { error ->
                    // Image upload failed, provide error message
                    _profileState.value =
                        UserProfileState.Error.Generic(
                            error.message ?: "Failed to upload profile image"
                        )
                }
            )
        }
    }
}
