package edu.utap.auth.repository

import com.google.firebase.auth.FirebaseUser
import edu.utap.auth.db.UserEntity
import kotlinx.coroutines.flow.Flow

interface AuthRepositoryInterface {
    // Remote authentication methods
    suspend fun registerUser(email: String, password: String, name: String): Result<FirebaseUser>
    suspend fun loginUser(email: String, password: String): Result<FirebaseUser>
    suspend fun getCurrentUser(): FirebaseUser?
    suspend fun logout()
    
    // Local user management methods
    suspend fun createLocalUser(user: UserEntity): Result<UserEntity>
    suspend fun getLocalUserByEmail(email: String): Result<UserEntity>
    suspend fun getLocalUserById(userId: String): Result<UserEntity>
    suspend fun updateLocalUser(user: UserEntity): Result<UserEntity>
    
    // Sync methods
    suspend fun syncUserToLocal(remoteUser: FirebaseUser): Result<UserEntity>
    suspend fun isAuthenticated(): Boolean
    
    // Observe user flow
    fun observeLocalUsers(): Flow<List<UserEntity>>
} 