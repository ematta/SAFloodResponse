package edu.utap.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.models.FirestoreUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining authentication and user management operations.
 *
 * Implementations handle Firebase Authentication, Firestore user profiles,
 * and synchronization between them.
 */
interface AuthRepositoryInterface {
    // Authentication methods
    /**
     * Registers a new user with email, password, name, and role.
     *
     * @param email User's email.
     * @param password User's password.
     * @param name User's display name.
     * @param role User role.
     * @return Result containing the created [FirebaseUser] or an error.
     */
    suspend fun registerUser(
        email: String,
        password: String,
        name: String,
        role: String
    ): Result<FirebaseUser>

    /**
     * Logs in an existing user.
     *
     * @param email User's email.
     * @param password User's password.
     * @return Result containing the authenticated [FirebaseUser] or an error.
     */
    suspend fun loginUser(email: String, password: String): Result<FirebaseUser>

    /**
     * Gets the currently authenticated Firebase user.
     *
     * @return The [FirebaseUser], or null if not authenticated.
     */
    suspend fun getCurrentUser(): FirebaseUser?

    /**
     * Logs out the current user.
     */
    suspend fun logout()

    /**
     * Sends a password reset email.
     *
     * @param email User's email.
     * @return Result indicating success or error.
     */
    suspend fun resetPassword(email: String): Result<Unit>

    // User management methods
    /**
     * Creates a new user profile in Firestore.
     *
     * @param user The [FirestoreUser] profile to create.
     * @return Result containing the created profile or an error.
     */
    suspend fun createUser(user: FirestoreUser): Result<FirestoreUser>

    /**
     * Retrieves a user profile by email.
     *
     * @param email The user's email.
     * @return Result containing the [FirestoreUser] or an error.
     */
    suspend fun getUserByEmail(email: String): Result<FirestoreUser>

    /**
     * Retrieves a user profile by user ID.
     *
     * @param userId The user's ID.
     * @return Result containing the [FirestoreUser] or an error.
     */
    suspend fun getUserById(userId: String): Result<FirestoreUser>

    /**
     * Updates an existing user profile.
     *
     * @param user The updated [FirestoreUser].
     * @return Result containing the updated profile or an error.
     */
    suspend fun updateUser(user: FirestoreUser): Result<FirestoreUser>

    // Sync methods
    /**
     * Synchronizes a Firebase Authentication user with Firestore profile.
     *
     * @param remoteUser The Firebase Authentication user.
     * @return Result containing the synced [FirestoreUser] or an error.
     */
    suspend fun syncUser(remoteUser: FirebaseUser): Result<FirestoreUser>

    /**
     * Checks if a user is currently authenticated.
     *
     * @return `true` if authenticated, `false` otherwise.
     */
    suspend fun isAuthenticated(): Boolean

    // Observe users
    /**
     * Observes the list of user profiles in Firestore.
     *
     * @return A [Flow] emitting lists of [FirestoreUser]s.
     */
    fun observeUsers(): Flow<List<FirestoreUser>>
}

// Concrete implementation
class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepositoryInterface {

    private val usersCollection get() = firestore.collection("users")

    override suspend fun registerUser(
        email: String,
        password: String,
        name: String,
        role: String
    ): Result<FirebaseUser> {
        return try {
            val authResult = com.google.android.gms.tasks.Tasks.await(
                firebaseAuth.createUserWithEmailAndPassword(email, password)
            )
            val user = authResult.user ?: return Result.failure(Exception("User creation failed"))
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            com.google.android.gms.tasks.Tasks.await(user.updateProfile(profileUpdates))

            val firestoreUser = FirestoreUser(
                userId = user.uid,
                email = email,
                name = name,
                role = role
            )
            createUser(firestoreUser)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginUser(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = com.google.android.gms.tasks.Tasks.await(
                firebaseAuth.signInWithEmailAndPassword(email, password)
            )
            val user = authResult.user ?: return Result.failure(Exception("Authentication failed"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override suspend fun resetPassword(email: String): Result<Unit> = try {
        com.google.android.gms.tasks.Tasks.await(
            firebaseAuth.sendPasswordResetEmail(email)
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createUser(user: FirestoreUser): Result<FirestoreUser> = try {
        com.google.android.gms.tasks.Tasks.await(
            usersCollection.document(user.userId).set(user)
        )
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserByEmail(email: String): Result<FirestoreUser> {
        return try {
            val querySnapshot = com.google.android.gms.tasks.Tasks.await(
                usersCollection.whereEqualTo("email", email).get()
            )
            val doc = querySnapshot.documents.firstOrNull()
                ?: return Result.failure(Exception("User not found"))
            val user = doc.toObject(FirestoreUser::class.java)
                ?: return Result.failure(Exception("Failed to parse user"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserById(userId: String): Result<FirestoreUser> {
        return try {
            val docSnapshot = com.google.android.gms.tasks.Tasks.await(
                usersCollection.document(userId).get()
            )
            if (!docSnapshot.exists()) {
                return Result.failure(Exception("User not found"))
            }
            val user = docSnapshot.toObject(FirestoreUser::class.java)
                ?: return Result.failure(Exception("Failed to parse user"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(user: FirestoreUser): Result<FirestoreUser> = try {
        com.google.android.gms.tasks.Tasks.await(
            usersCollection.document(user.userId).set(user)
        )
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun syncUser(remoteUser: FirebaseUser): Result<FirestoreUser> =
        getUserById(remoteUser.uid).fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                val newUser = FirestoreUser(
                    userId = remoteUser.uid,
                    email = remoteUser.email ?: "",
                    name = remoteUser.displayName ?: "",
                    role = "user"
                )
                createUser(newUser)
            }
        )

    override suspend fun isAuthenticated(): Boolean = firebaseAuth.currentUser != null

    override fun observeUsers(): Flow<List<FirestoreUser>> = kotlinx.coroutines.flow.callbackFlow {
        val listenerRegistration = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val users = snapshot.documents.mapNotNull { it.toObject(FirestoreUser::class.java) }
                trySend(users).isSuccess
            }
        }
        awaitClose { listenerRegistration.remove() }
    }
}
