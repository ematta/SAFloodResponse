package edu.utap.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import edu.utap.auth.db.UserDao
import edu.utap.auth.db.UserEntity
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.auth.repository.toUserEntity
import edu.utap.auth.repository.updateFromFirebaseUser
import edu.utap.user.UserProfile
import edu.utap.user.UserRepository
import edu.utap.user.FirebaseUserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Implementation of AuthRepositoryInterface that handles both Firebase Authentication
 * and local user data management.
 */
class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userDao: UserDao,
    private val userRepository: UserRepository = FirebaseUserRepository()
) : AuthRepositoryInterface {

    override suspend fun registerUser(email: String, password: String, name: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Registration failed")
            
            // Update the display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            
            user.updateProfile(profileUpdates).await()
            
            // Create user in local DB
            val userEntity = user.toUserEntity()
            createLocalUser(userEntity)
            
            // Create user profile in Firestore
            val userProfile = UserProfile(
                uid = user.uid,
                displayName = name,
                email = email
            )
            val profileResult: Result<UserProfile> = userRepository.createUserProfile(userProfile)
            if (profileResult.isFailure) {
                throw profileResult.exceptionOrNull() ?: Exception("Failed to create Firestore profile")
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginUser(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Login failed")
            
            // Sync user data to local
            syncUserToLocal(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createLocalUser(user: UserEntity): Result<UserEntity> {
        return try {
            userDao.insertUser(user)
            
            // Check if a Firestore profile already exists
            val profileResult = userRepository.getUserProfile(user.userId)
            if (profileResult.isFailure) {
                // Create the Firestore profile
                val firestoreProfile = UserProfile(
                    uid = user.userId,
                    displayName = user.name,
                    email = user.email,
                    photoUrl = user.profilePic ?: ""
                )
                userRepository.createUserProfile(firestoreProfile)
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLocalUserByEmail(email: String): Result<UserEntity> {
        return try {
            val user = userDao.getUserByEmail(email) ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLocalUserById(userId: String): Result<UserEntity> {
        return try {
            val user = userDao.getUserById(userId) ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLocalUser(user: UserEntity): Result<UserEntity> {
        return try {
            userDao.updateUser(user)
            
            // Update the Firestore profile as well
            val firestoreProfile = UserProfile(
                uid = user.userId,
                displayName = user.name,
                email = user.email,
                photoUrl = user.profilePic ?: ""
            )
            userRepository.updateUserProfile(firestoreProfile)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncUserToLocal(remoteUser: FirebaseUser): Result<UserEntity> {
        return try {
            // Check if user exists locally
            val existingUser = userDao.getUserById(remoteUser.uid)
            
            // Get Firestore profile to ensure we have the most up-to-date data
            val firestoreProfileResult = userRepository.getUserProfile(remoteUser.uid)
            
            val userEntity = if (existingUser != null) {
                // Update existing user
                val updatedUser = existingUser.updateFromFirebaseUser(remoteUser)
                
                // If we have a Firestore profile, use that data to update as well
                val finalUser = if (firestoreProfileResult.isSuccess) {
                    val profile = firestoreProfileResult.getOrNull()!!
                    updatedUser.copy(
                        name = profile.displayName.takeIf { it.isNotEmpty() } ?: updatedUser.name,
                        profilePic = profile.photoUrl.takeIf { it.isNotEmpty() } ?: updatedUser.profilePic
                    )
                } else {
                    updatedUser
                }
                
                userDao.updateUser(finalUser)
                finalUser
            } else {
                // Create new user
                val newUser = remoteUser.toUserEntity()
                
                // If we have a Firestore profile, use that data
                val finalUser = if (firestoreProfileResult.isSuccess) {
                    val profile = firestoreProfileResult.getOrNull()!!
                    newUser.copy(
                        name = profile.displayName.takeIf { it.isNotEmpty() } ?: newUser.name,
                        profilePic = profile.photoUrl.takeIf { it.isNotEmpty() } ?: newUser.profilePic
                    )
                } else {
                    newUser
                }
                
                userDao.insertUser(finalUser)
                finalUser
            }
            
            // If Firestore profile doesn't exist, create it
            if (firestoreProfileResult.isFailure) {
                val profile = UserProfile(
                    uid = remoteUser.uid,
                    displayName = userEntity.name,
                    email = userEntity.email,
                    photoUrl = userEntity.profilePic ?: ""
                )
                userRepository.createUserProfile(profile)
            }
            
            Result.success(userEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override fun observeLocalUsers(): Flow<List<UserEntity>> {
        return userDao.getAllUsers()
    }
}