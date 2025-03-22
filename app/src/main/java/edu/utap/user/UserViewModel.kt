package edu.utap.user

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val storageUtil: FirebaseStorageUtil = FirebaseStorageUtil()
) : ViewModel() {

    private val _profileState = MutableStateFlow<UserProfileState>(UserProfileState.Idle)
    val profileState: StateFlow<UserProfileState> = _profileState

    fun createUserProfile(userProfile: UserProfile) {
        _profileState.value = UserProfileState.Loading
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
        viewModelScope.launch {
            val result = userRepository.updateDisplayName(uid, displayName)
            result.fold(
                onSuccess = {
                    getUserProfile(uid) // Refresh the profile
                },
                onFailure = { error ->
                    _profileState.value = UserProfileState.Error(error.message ?: "Failed to update display name")
                }
            )
        }
    }

    fun updatePhotoUrl(uid: String, photoUrl: String) {
        _profileState.value = UserProfileState.Loading
        viewModelScope.launch {
            val result = userRepository.updatePhotoUrl(uid, photoUrl)
            result.fold(
                onSuccess = {
                    getUserProfile(uid) // Refresh the profile
                },
                onFailure = { error ->
                    _profileState.value = UserProfileState.Error(error.message ?: "Failed to update photo URL")
                }
            )
        }
    }
    
    fun uploadProfileImage(context: Context, imageUri: Uri, uid: String) {
        _profileState.value = UserProfileState.Loading
        viewModelScope.launch {
            val uploadResult = storageUtil.uploadProfileImage(context, imageUri, uid)
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