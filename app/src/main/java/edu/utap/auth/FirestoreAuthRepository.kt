package edu.utap.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.models.FirestoreUser
import edu.utap.repository.BaseRepository
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
) : BaseRepository(), AuthRepositoryInterface {
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
    ): Result<FirebaseUser> = safeNetworkCall {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("Registration failed")

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()

        try {
            user.updateProfile(profileUpdates).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating display name", e)
        }

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
        safeNetworkCall {
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
    override suspend fun resetPassword(email: String): Result<Unit> = safeNetworkCall {
        firebaseAuth.sendPasswordResetEmail(email).await()
        Unit
    }

    /**
     * Creates a new user profile in Firestore.
     *
     * @param user The user profile to be created.
     * @return [Result] containing the created [FirestoreUser] on success, or an exception on failure.
     */
    override suspend fun createUser(user: FirestoreUser): Result<FirestoreUser> = safeFirestoreCall {
        usersCollection.document(user.userId)
            .set(user)
            .await()
        user
    }

    /**
     * Retrieves a user by their email from Firestore.
     *
     * @param email The email of the user to retrieve.
     * @return [Result] containing the [FirestoreUser] on success, or an exception on failure.
     */
    override suspend fun getUserByEmail(email: String): Result<FirestoreUser> = safeFirestoreCall {
        val query = usersCollection.whereEqualTo("email", email).limit(1).get().await()
        val user = query.documents.firstOrNull()?.toDomainObject<FirestoreUser>()
            ?: throw Exception("User not found")
        user
    }

    /**
     * Retrieves a user by their ID from Firestore.
     *
     * @param userId The ID of the user to retrieve.
     * @return [Result] containing the [FirestoreUser] on success, or an exception on failure.
     */
    override suspend fun getUserById(userId: String): Result<FirestoreUser> = safeFirestoreCall {
        val document = usersCollection.document(userId).get().await()
        val user = document.toDomainObject<FirestoreUser>()
            ?: throw Exception("User not found")
        user
    }

    /**
     * Updates a user's profile in Firestore.
     *
     * @param user The updated user profile.
     * @return [Result] containing the updated [FirestoreUser] on success, or an exception on failure.
     */
    override suspend fun updateUser(user: FirestoreUser): Result<FirestoreUser> = safeFirestoreCall {
        usersCollection.document(user.userId)
            .set(user)
            .await()
        user
    }

    /**
     * Synchronizes a Firebase user with Firestore.
     *
     * This method retrieves user data from Firestore and updates the local user
     *
     * @param remoteUser The Firebase user to synchronize.
     * @return [Result] containing the synchronized [FirestoreUser] on success, or an exception on failure.
     */
    override suspend fun syncUser(remoteUser: FirebaseUser): Result<FirestoreUser> = safeFirestoreCall {
        val firestoreUser = FirestoreUser.fromFirebaseUser(remoteUser)
        usersCollection.document(firestoreUser.userId)
            .set(firestoreUser)
            .await()
        firestoreUser
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
                document.toDomainObject<FirestoreUser>()
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
}
