package edu.utap.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.models.FirestoreUser
import edu.utap.repository.AuthRepositoryInterface
import edu.utap.utils.FirebaseErrorMapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreAuthRepository"
/**
 * Repository implementation handling user authentication and profile management
 * using Firebase Authentication and Firestore.
 *
 * This class provides methods to register and authenticate users, synchronize
 * user data with Firestore, and manage user roles.
 *
 * @property firebaseAuth Firebase Authentication instance (default: singleton)
 * @property firestore Firebase Firestore instance (default: singleton)
 * @property userRepository Repository for user profile operations (default: FirebaseUserRepository)
 */
class FirestoreAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepositoryInterface {
    private val usersCollection = firestore.collection("users")

    /**
     * Registers a new user with email, password, display name, and role.
     *
     * This method performs the following steps:
     * 1. Creates a Firebase Authentication account.
     * 2. Updates the user's display name in Firebase Auth.
     * 3. Creates a corresponding user profile in Firestore.
     *
     * @param email User's email address.
     * @param password User's password.
     * @param name Display name to associate with the user.
     * @param role User role (e.g., regular, volunteer, admin).
     * @return [Result] containing the created [FirebaseUser] on success, or an exception on failure.
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
            Log.e(TAG, "Error updating display name", e)
        }
        // Step 3: Create user profile in Firestore
        syncUser(user)
        user
    }

    /**
     * Authenticates an existing user with email and password.
     *
     * This method signs in the user via Firebase Authentication and synchronizes
     * their profile data with Firestore.
     *
     * @param email User's email address.
     * @param password User's password.
     * @return [Result] containing the authenticated [FirebaseUser] on success, or an exception on failure.
     */
    override suspend fun loginUser(email: String, password: String): Result<FirebaseUser> =
        executeFirebaseAuthOperation {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Login failed")
            syncUser(user)
            user
        }

    /**
     * Retrieves the currently authenticated user.
     *
     * @return [FirebaseUser] if the user is authenticated, null otherwise.
     */
    override suspend fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    /**
     * Signs out the currently authenticated user.
     *
     * @return [Unit]
     */
    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    /**
     * Sends a password reset email to the provided email address.
     *
     * @param email The email address to send the password reset link to.
     * @return [Result] containing [Unit] on success, or an exception on failure.
     */
    override suspend fun resetPassword(email: String): Result<Unit> = try {
        firebaseAuth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }

    /**
     * Creates a new user profile in Firestore.
     *
     * @param user The user profile to be created.
     * @return [Result] containing the created [FirestoreUser] on success, or an exception on failure.
     */
    override suspend fun createUser(user: FirestoreUser): Result<FirestoreUser> = try {
        usersCollection.document(user.userId)
            .set(user)
            .await()
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }

    /**
     * Retrieves a user by their email from Firestore.
     *
     * @param email The email of the user to retrieve.
     * @return [Result] containing the [FirestoreUser] on success, or an exception on failure.
     */
    override suspend fun getUserByEmail(email: String): Result<FirestoreUser> = try {
        val query = usersCollection.whereEqualTo("email", email).limit(1).get().await()
        val user = query.documents.firstOrNull()?.toObject(FirestoreUser::class.java)
            ?: throw Exception("User not found")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Retrieves a user by their ID from Firestore.
     *
     * @param userId The ID of the user to retrieve.
     * @return [Result] containing the [FirestoreUser] on success, or an exception on failure.
     */
    override suspend fun getUserById(userId: String): Result<FirestoreUser> = try {
        val document = usersCollection.document(userId).get().await()
        val user = document.toObject(FirestoreUser::class.java)
            ?: throw Exception("User not found")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Updates a user's profile in Firestore.
     *
     * @param user The updated user profile.
     * @return [Result] containing the updated [FirestoreUser] on success, or an exception on failure.
     */
    override suspend fun updateUser(user: FirestoreUser): Result<FirestoreUser> = try {
        usersCollection.document(user.userId)
            .set(user)
            .await()
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Synchronizes a Firebase user with Firestore.
     *
     * This method retrieves user data from Firestore and updates the local user
     *
     * @param remoteUser The Firebase user to synchronize.
     * @return [Result] containing the synchronized [FirestoreUser] on success, or an exception on failure.
     */
    override suspend fun syncUser(remoteUser: FirebaseUser): Result<FirestoreUser> = try {
        val firestoreUser = FirestoreUser.fromFirebaseUser(remoteUser)
        usersCollection.document(firestoreUser.userId)
            .set(firestoreUser)
            .await()
        Result.success(firestoreUser)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Checks if the current user is authenticated.
     *
     * @return [Boolean] indicating if the user is authenticated.
     */
    override suspend fun isAuthenticated(): Boolean = firebaseAuth.currentUser != null

    /**
     * Retrieves a list of all users from Firestore.
     *
     * @return [Flow] emitting a list of [FirestoreUser] objects.
     */
    override fun observeUsers(): Flow<List<FirestoreUser>> = callbackFlow {
        val listener = usersCollection.addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            val users = querySnapshot?.documents?.mapNotNull { document ->
                document.toObject(FirestoreUser::class.java)
            } ?: emptyList()
            
            trySend(users)
        }
        
        awaitClose { listener.remove() }
    }

    /**
     * Executes an authentication operation using Firebase Authentication.
     *
     * @param authOperation The authentication operation to be executed.
     * @return [Result] containing the result of the authentication operation on success,
     */
    private suspend fun <T> executeFirebaseAuthOperation(
        authOperation: suspend () -> T
    ): Result<T> = try {
        Result.success(authOperation())
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }
}
