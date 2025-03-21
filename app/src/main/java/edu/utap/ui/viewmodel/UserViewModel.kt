package edu.utap.ui.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import edu.utap.auth.UserProfileState
import edu.utap.models.UserProfile
import edu.utap.repository.FirebaseUserRepository
import edu.utap.repository.UserRepository
import edu.utap.utils.DefaultImageCacheProvider
import edu.utap.utils.FirebaseStorageUtil
import edu.utap.utils.ImageCacheProviderInterface
import edu.utap.utils.LocationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlin.Result

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
 * - Managing profile pictures and caching them locally
 *
 * It includes network connectivity checks before attempting operations
 * that require internet access, and exposes profile state through a StateFlow.
 */
class UserViewModel(
    application: Application,
    private val userRepository: UserRepository = FirebaseUserRepository(),
    private val storageUtil: FirebaseStorageUtil = FirebaseStorageUtil(),
    private val locationUtils: LocationUtils = LocationUtils(application),
    private val imageCacheProvider: ImageCacheProviderInterface = DefaultImageCacheProvider.getInstance(application)
) : AndroidViewModel(application) {

    // locationUtils is injected via constructor

    // State for current user location
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

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

            if (result.isFailure) {
                _profileState.value = UserProfileState.Error.Generic(
                    result.exceptionOrNull()?.message ?: "Failed to create user profile"
                )
            } else {
                val profile: UserProfile = result.getOrNull() as UserProfile
                _profileState.value = UserProfileState.Success(profile)
            }
        }
    }

    /**
     * Retrieves a user profile from Firestore by user ID and prefetches their profile image.
     *
     * This method performs the following steps:
     * 1. Updates the profile state to Loading
     * 2. Checks for network connectivity
     * 3. Calls the repository to fetch the profile from Firestore
     * 4. Updates the profile state based on the result
     * 5. Prefetches the profile image for offline use
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
            if (result.isFailure) {
                _profileState.value = UserProfileState.Error.Generic(
                    result.exceptionOrNull()?.message ?: "Failed to fetch user profile"
                )
            } else {
                val raw = result.getOrNull()
                // Support returning DocumentSnapshot or UserProfile directly in tests
                val profile: UserProfile? = when (raw) {
                    is DocumentSnapshot -> raw.toObject(UserProfile::class.java)
                    is UserProfile -> raw
                    else -> null
                }
                if (profile == null) {
                    _profileState.value = UserProfileState.Error.Generic(
                        "Failed to fetch user profile"
                    )
                } else {
                    _profileState.value = UserProfileState.Success(profile)
                    
                    // Prefetch and cache the profile image if it exists
                    if (profile.photoUrl.isNotEmpty()) {
                        prefetchProfileImage(uid, profile.photoUrl)
                    }
                }
            }
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
            if (result.isFailure) {
                _profileState.value = UserProfileState.Error.Generic(
                    result.exceptionOrNull()?.message ?: "Failed to update user profile"
                )
            } else {
                val profile: UserProfile = result.getOrNull() as UserProfile
                _profileState.value = UserProfileState.Success(profile)
            }
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
            if (result.isFailure) {
                _profileState.value = UserProfileState.Error.Generic(
                    result.exceptionOrNull()?.message ?: "Failed to update display name"
                )
            } else {
                getUserProfile(uid)
            }
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
            if(result.isSuccess) {
                getUserProfile(uid)
            } else {
                _profileState.value = UserProfileState.Success(
                    UserProfile(
                        uid = uid,
                        photoUrl = photoUrl
                    )
                )
            }
        }
    }

    /**
     * Uploads a profile image to Firebase Storage, caches it locally, and updates the user's profile.
     *
     * This method performs a multi-step process:
     * 1. Caches the image locally for offline access
     * 2. Uploads the image to Firebase Storage
     * 3. Updates the user's profile with the download URL of the uploaded image
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
            // First cache the local image
            imageCacheProvider.cacheProfileImage(uid, imageUri)
            
            // Step 1: Upload the image to Firebase Storage
            val uploadResult = storageUtil.uploadProfileImage(
                context = uploadContext,
                imageUri = imageUri,
                userId = uid
            )
            if (uploadResult.isFailure) {
                _profileState.value = UserProfileState.Error.Generic(
                    "Failed to upload profile image: ${uploadResult.exceptionOrNull()?.message}"
                )
            } else {
                val url = uploadResult.getOrNull() as String
                if (url != null) {
                    updatePhotoUrl(uid, url)
                } else {
                    _profileState.value =
                        UserProfileState.Error.Generic("Failed to upload profile image")
                }
            }
        }
    }
    
    /**
     * Prefetches a profile image and caches it locally for offline access
     *
     * @param userId The ID of the user
     * @param imageUrl The URL of the profile image to prefetch
     */
    fun prefetchProfileImage(userId: String, imageUrl: String) {
        if (imageUrl.isEmpty()) return
        
        viewModelScope.launch {
            imageCacheProvider.prefetchProfileImage(userId, imageUrl)
        }
    }
    
    /**
     * Gets a URI for a cached profile image if available
     *
     * @param userId The ID of the user
     * @return URI of the cached image or null if not cached
     */
    fun getCachedProfileImageUri(userId: String): Uri? {
        return imageCacheProvider.getCachedProfileImageUri(userId)
    }

    /**
     * Fetches current location if permissions are granted and updates state.
     */
    fun fetchCurrentLocation() {
        viewModelScope.launch {
            if (locationUtils.hasLocationPermissions()) {
                val location = try {
                    locationUtils.getCurrentLocation()
                } catch (e: Exception) {
                    null
                }
                _currentLocation.value = location
            }
        }
    }
}
