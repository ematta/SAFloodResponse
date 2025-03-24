package edu.utap.user

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.utap.auth.utils.ApplicationContextProvider
import edu.utap.auth.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UserProfileState {
    object Loading : UserProfileState()
    object Idle : UserProfileState()
    data class Success(val userProfile: UserProfile) : UserProfileState()
    data class Error(val message: String) : UserProfileState()
}

class UserViewModel(
    private val userRepository: UserRepository = FirebaseUserRepository(),
    private val storageUtil: FirebaseStorageUtilInterface = StorageUtilProvider.getStorageUtil(),
    private val networkUtils: edu.utap.auth.utils.NetworkUtilsInterface = edu.utap.auth.utils.NetworkUtilsProvider.getNetworkUtils()
) : ViewModel() {

    private val _profileState = MutableStateFlow<UserProfileState>(UserProfileState.Idle)
    val profileState: StateFlow<UserProfileState> = _profileState

    fun createUserProfile(userProfile: UserProfile) {
        _profileState.value = UserProfileState.Loading
        
        // Check for network connectivity before attempting to create profile
        if (!networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
            _profileState.value = UserProfileState.Error("No internet connection. Please check your network settings and try again.")
            return
        }
        
        viewModelScope.launch {
            val result = userRepository.createUserProfile(userProfile)
            result.fold(
                onSuccess = { profile ->
                    _profileState.value = UserProfileState.Success(profile)
                },
                onFailure = { error ->
                    _profileState.value = UserProfileState.Error(error.message ?: "Failed to create user profile")
                }
            )
        }
    }

    fun getUserProfile(uid: String) {
        _profileState.value = UserProfileState.Loading
        
        // Check for network connectivity before attempting to get profile
        if (!networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
            _profileState.value = UserProfileState.Error("No internet connection. Please check your network settings and try again.")
            return
        }
        
        viewModelScope.launch {
            val result = userRepository.getUserProfile(uid)
            result.fold(
                onSuccess = { profile ->
                    _profileState.value = UserProfileState.Success(profile)
                },
                onFailure = { error ->
                    _profileState.value = UserProfileState.Error(error.message ?: "Failed to get user profile")
                }
            )
        }
    }

    fun updateUserProfile(userProfile: UserProfile) {
        _profileState.value = UserProfileState.Loading
        
        // Check for network connectivity before attempting to update profile
        if (!networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
            _profileState.value = UserProfileState.Error("No internet connection. Please check your network settings and try again.")
            return
        }
        
        viewModelScope.launch {
            val result = userRepository.updateUserProfile(userProfile)
            result.fold(
                onSuccess = { profile ->
                    _profileState.value = UserProfileState.Success(profile)
                },
                onFailure = { error ->
                    _profileState.value = UserProfileState.Error(error.message ?: "Failed to update user profile")
                }
            )
        }
    }

    fun updateDisplayName(uid: String, displayName: String) {
        _profileState.value = UserProfileState.Loading
        
        // Check for network connectivity before attempting to update display name
        if (!networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
            _profileState.value = UserProfileState.Error("No internet connection. Please check your network settings and try again.")
            return
        }
        
        viewModelScope.launch {
            val result = userRepository.updateDisplayName(uid, displayName)
            result.fold(
                onSuccess = {
                    // Only refresh profile if we have network connectivity
                    if (networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
                        getUserProfile(uid) // Refresh the profile
                    } else {
                        _profileState.value = UserProfileState.Success(UserProfile(uid = uid, displayName = displayName))
                    }
                },
                onFailure = { error ->
                    _profileState.value = UserProfileState.Error(error.message ?: "Failed to update display name")
                }
            )
        }
    }

    fun updatePhotoUrl(uid: String, photoUrl: String) {
        _profileState.value = UserProfileState.Loading
        
        // Check for network connectivity before attempting to update photo URL
        if (!networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
            _profileState.value = UserProfileState.Error("No internet connection. Please check your network settings and try again.")
            return
        }
        
        viewModelScope.launch {
            val result = userRepository.updatePhotoUrl(uid, photoUrl)
            result.fold(
                onSuccess = {
                    // Only refresh profile if we have network connectivity
                    if (networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
                        getUserProfile(uid) // Refresh the profile
                    } else {
                        _profileState.value = UserProfileState.Success(UserProfile(uid = uid, photoUrl = photoUrl))
                    }
                },
                onFailure = { error ->
                    _profileState.value = UserProfileState.Error(error.message ?: "Failed to update photo URL")
                }
            )
        }
    }
    
    fun uploadProfileImage(uploadContext: Context, imageUri: Uri, uid: String) {
        _profileState.value = UserProfileState.Loading
        
        // Check for network connectivity before attempting to upload image
        if (!networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
            _profileState.value = UserProfileState.Error("No internet connection. Please check your network settings and try again.")
            return
        }
        
        viewModelScope.launch {
            val uploadResult = storageUtil.uploadProfileImage(uploadContext, imageUri, uid)
            uploadResult.fold(
                onSuccess = { downloadUrl ->
                    // Update the photo URL in the user profile
                    updatePhotoUrl(uid, downloadUrl)
                },
                onFailure = { error ->
                    _profileState.value = UserProfileState.Error(error.message ?: "Failed to upload profile image")
                }
            )
        }
    }
}