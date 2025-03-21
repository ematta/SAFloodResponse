package edu.utap.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface UserRepository {
    suspend fun createUserProfile(userProfile: UserProfile): Result<UserProfile>
    suspend fun getUserProfile(uid: String): Result<UserProfile>
    suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile>
    suspend fun updateDisplayName(uid: String, displayName: String): Result<Unit>
    suspend fun updatePhotoUrl(uid: String, photoUrl: String): Result<Unit>
}

class FirebaseUserRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun createUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return try {
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
    }

    override suspend fun getUserProfile(uid: String): Result<UserProfile> {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document != null && document.exists()) {
                val userProfile = document.toObject(UserProfile::class.java)
                userProfile?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Failed to parse user profile"))
            } else {
                Result.failure(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return try {
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
    }
    
    override suspend fun updateDisplayName(uid: String, displayName: String): Result<Unit> {
        return try {
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
    }
    
    override suspend fun updatePhotoUrl(uid: String, photoUrl: String): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser?.uid == uid) {
                val profileUpdates = userProfileChangeRequest {
                    this.photoUri = android.net.Uri.parse(photoUrl)
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
} 