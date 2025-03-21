package edu.utap.auth.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import edu.utap.auth.db.UserDao
import edu.utap.auth.db.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userDao: UserDao
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

    override suspend fun createLocalUser(user: UserEntity): Result<UserEntity> {
        return try {
            userDao.insertUser(user)
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
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncUserToLocal(remoteUser: FirebaseUser): Result<UserEntity> {
        return try {
            // Check if user exists locally
            val existingUser = userDao.getUserById(remoteUser.uid)
            
            val userEntity = if (existingUser != null) {
                // Update existing user
                val updatedUser = existingUser.updateFromFirebaseUser(remoteUser)
                userDao.updateUser(updatedUser)
                updatedUser
            } else {
                // Create new user
                val newUser = remoteUser.toUserEntity()
                userDao.insertUser(newUser)
                newUser
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