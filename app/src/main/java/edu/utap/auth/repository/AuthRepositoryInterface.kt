package edu.utap.auth.repository

import com.google.firebase.auth.FirebaseUser
import edu.utap.auth.model.FirestoreUser
import kotlinx.coroutines.flow.Flow

interface AuthRepositoryInterface {
    // Authentication methods
    suspend fun registerUser(
        email: String,
        password: String,
        name: String,
        role: String
    ): Result<FirebaseUser>
    suspend fun loginUser(email: String, password: String): Result<FirebaseUser>
    suspend fun getCurrentUser(): FirebaseUser?
    suspend fun logout()
    suspend fun resetPassword(email: String): Result<Unit>

    // User management methods
    suspend fun createUser(user: FirestoreUser): Result<FirestoreUser>
    suspend fun getUserByEmail(email: String): Result<FirestoreUser>
    suspend fun getUserById(userId: String): Result<FirestoreUser>
    suspend fun updateUser(user: FirestoreUser): Result<FirestoreUser>

    // Sync methods
    suspend fun syncUser(remoteUser: FirebaseUser): Result<FirestoreUser>
    suspend fun isAuthenticated(): Boolean

    // Observe users
    fun observeUsers(): Flow<List<FirestoreUser>>
}
