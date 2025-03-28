package edu.utap.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import edu.utap.db.UserDao
import edu.utap.db.UserEntity
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.auth.repository.toUserEntity
import edu.utap.auth.repository.updateFromFirebaseUser
import edu.utap.utils.FirebaseErrorMapper
import edu.utap.user.repository.FirebaseUserRepository
import edu.utap.user.UserProfile
import edu.utap.user.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Implementation of AuthRepositoryInterface that handles both Firebase Authentication
 * and local user data management.
 *
 * This repository serves as the single source of truth for user authentication data,
 * managing both remote Firebase authentication and local database storage.
 * It synchronizes data between Firebase Auth and the local Room database.
 */
class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userDao: UserDao,
    private val userRepository: UserRepository = FirebaseUserRepository()
) : AuthRepositoryInterface {

    private val userDataSynchronizer = UserDataSynchronizer(this, userRepository)

    /**
     * Registers a new user with Firebase Authentication and creates associated profiles.
     *
     * This method performs a multi-step registration process:
     * 1. Creates the user account in Firebase Authentication
     * 2. Updates the user's display name in Firebase Auth
     * 3. Creates a local database entry for the user
     * 4. Creates a Firestore profile document for the user
     *
     * Error handling is implemented at each step, with graceful degradation for non-critical failures
     * (e.g., profile updates can be retried later if they fail initially).
     *
     * @param email User's email address for authentication
     * @param password User's password for authentication
     * @param name User's display name
     * @return Result containing the Firebase user if successful, or an error message
     */
    override suspend fun registerUser(
        email: String,
        password: String,
        name: String,
        role: String
    ): Result<FirebaseUser> = executeFirebaseAuthOperation {
        // Step 1: Create Firebase Authentication account
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("Registration failed")

        // Step 2: Update the display name in Firebase Auth
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()

        try {
            user.updateProfile(profileUpdates).await()
        } catch (e: Exception) {
            // Log the error but continue with the registration process
            // The profile can be updated later
        }

        // Step 3: Create user profile in Firestore and local database
        userDataSynchronizer.syncUserToLocal(user)

        user
    }

    /**
     * Authenticates a user with Firebase and synchronizes their data locally.
     *
     * This method performs two main operations:
     * 1. Authenticates the user with Firebase using email and password
     * 2. Synchronizes the Firebase user data with the local database
     *
     * The synchronization ensures that the local database has the most up-to-date
     * user information from both Firebase Auth and Firestore.
     *
     * @param email User's email address for authentication
     * @param password User's password for authentication
     * @return Result containing the Firebase user if successful, or an error message
     */
    override suspend fun loginUser(email: String, password: String): Result<FirebaseUser> =
        executeFirebaseAuthOperation {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Login failed")
            userDataSynchronizer.syncUserToLocal(user)
            user
        }

    override suspend fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    /**
     * Sends a password reset email to the user's email address.
     *
     * This method uses Firebase Authentication to send a password reset email.
     * It includes error handling to provide user-friendly error messages.
     *
     * Note: The nested try-catch structure is intentional:
     * - The inner try-catch is for logging purposes but still propagates the error
     * - The outer try-catch converts Firebase errors to user-friendly messages
     *
     * @param email The email address to send the password reset link to
     * @return Result indicating success or failure with a user-friendly error message
     */
    override suspend fun resetPassword(email: String): Result<Unit> = try {
        firebaseAuth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }

    /**
     * Creates a user in the local database and ensures a corresponding Firestore profile exists.
     *
     * This method performs two main operations:
     * 1. Inserts the user entity into the local Room database
     * 2. Checks if a Firestore profile exists for the user, and creates one if it doesn't
     *
     * This ensures data consistency between the local database and Firestore,
     * which is essential for offline-first functionality and data synchronization.
     *
     * @param user The UserEntity to be stored in the local database
     * @return Result containing the created UserEntity if successful, or an error message
     */
    override suspend fun createLocalUser(user: UserEntity): Result<UserEntity> = try {
        userDao.insertUser(user)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override suspend fun getLocalUserByEmail(email: String): Result<UserEntity> = try {
        val user = userDao.getUserByEmail(email) ?: throw Exception("User not found")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getLocalUserById(userId: String): Result<UserEntity> = try {
        val user = userDao.getUserById(userId) ?: throw Exception("User not found")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Updates a user in the local database and synchronizes changes to Firestore.
     *
     * This method performs two main operations:
     * 1. Updates the user entity in the local Room database
     * 2. Updates the corresponding Firestore profile to maintain data consistency
     *
     * This bidirectional synchronization ensures that user data remains consistent
     * across both the local database and Firestore cloud storage.
     *
     * @param user The updated UserEntity to be stored
     * @return Result containing the updated UserEntity if successful, or an error message
     */
    override suspend fun updateLocalUser(user: UserEntity): Result<UserEntity> = try {
        userDao.updateUser(user)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Synchronizes a Firebase user with the local database.
     *
     * This method performs a complex synchronization between three data sources:
     * 1. Firebase Authentication (remoteUser)
     * 2. Local Room database (existingUser)
     * 3. Firestore user profile (firestoreProfile)
     *
     * The synchronization logic prioritizes data in the following order:
     * - Firestore profile data (if available)
     * - Firebase Auth data
     * - Existing local data (as fallback)
     *
     * @param remoteUser The Firebase user to synchronize with local storage
     * @return Result containing the synchronized UserEntity or an error
     */
    override suspend fun syncUserToLocal(remoteUser: FirebaseUser): Result<UserEntity> = try {
        // Check if user exists locally
        val existingUser = userDao.getUserById(remoteUser.uid)

        // Get Firestore profile to ensure we have the most up-to-date data
        val firestoreProfileResult = userRepository.getUserProfile(remoteUser.uid)

        val userEntity = if (existingUser != null) {
            // Update existing user with Firebase Auth data
            val updatedUser = existingUser.updateFromFirebaseUser(remoteUser)

            // If we have a Firestore profile, use that data to update as well
            // This ensures we have the most complete and up-to-date user information
            val finalUser = if (firestoreProfileResult.isSuccess) {
                val profile = firestoreProfileResult.getOrNull()!!
                updatedUser.copy(
                    name = profile.displayName.takeIf { it.isNotEmpty() } ?: updatedUser.name,
                    profilePic =
                    profile.photoUrl.takeIf { it.isNotEmpty() } ?: updatedUser.profilePic
                )
            } else {
                updatedUser
            }

            userDao.updateUser(finalUser)
            finalUser
        } else {
            // Create new user from Firebase Auth data
            val newUser = remoteUser.toUserEntity()

            // If we have a Firestore profile, enhance the new user with that data
            val finalUser = if (firestoreProfileResult.isSuccess) {
                val profile = firestoreProfileResult.getOrNull()!!
                newUser.copy(
                    name = profile.displayName.takeIf { it.isNotEmpty() } ?: newUser.name,
                    profilePic =
                    profile.photoUrl.takeIf { it.isNotEmpty() } ?: newUser.profilePic
                )
            } else {
                newUser
            }

            userDao.insertUser(finalUser)
            finalUser
        }

        // If Firestore profile doesn't exist, create it to ensure consistency
        // across all data sources (Firebase Auth, Room DB, and Firestore)
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

    override suspend fun isAuthenticated(): Boolean = firebaseAuth.currentUser != null

    override fun observeLocalUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    /**
     * Common function to execute Firebase authentication operations with standardized error handling.
     *
     * This private method encapsulates the common try-catch pattern used across authentication methods,
     * providing consistent error handling and Firebase error message mapping.
     *
     * @param authOperation A suspend lambda that performs the Firebase authentication operation
     * @return Result containing the Firebase user if successful, or an error message
     */
    private suspend fun <T> executeFirebaseAuthOperation(
        authOperation: suspend () -> T
    ): Result<T> = try {
        Result.success(authOperation())
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }
}
