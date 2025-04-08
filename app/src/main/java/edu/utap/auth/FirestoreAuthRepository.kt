package edu.utap.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.auth.model.FirestoreUser
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.user.UserProfile
import edu.utap.user.repository.FirebaseUserRepository
import edu.utap.user.repository.UserRepository
import edu.utap.utils.FirebaseErrorMapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val userRepository: UserRepository = FirebaseUserRepository()
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
            // Log the error but continue with the registration process
            // The profile can be updated later
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

    override suspend fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override suspend fun resetPassword(email: String): Result<Unit> = try {
        firebaseAuth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override suspend fun createUser(user: FirestoreUser): Result<FirestoreUser> = try {
        usersCollection.document(user.userId)
            .set(user)
            .await()
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override suspend fun getUserByEmail(email: String): Result<FirestoreUser> = try {
        val query = usersCollection.whereEqualTo("email", email).limit(1).get().await()
        val user = query.documents.firstOrNull()?.toObject(FirestoreUser::class.java)
            ?: throw Exception("User not found")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserById(userId: String): Result<FirestoreUser> = try {
        val document = usersCollection.document(userId).get().await()
        val user = document.toObject(FirestoreUser::class.java)
            ?: throw Exception("User not found")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateUser(user: FirestoreUser): Result<FirestoreUser> = try {
        usersCollection.document(user.userId)
            .set(user)
            .await()
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun syncUser(remoteUser: FirebaseUser): Result<FirestoreUser> = try {
        // Get Firestore profile to ensure we have the most up-to-date data
        val firestoreProfileResult = userRepository.getUserProfile(remoteUser.uid)

        val firestoreUser = if (firestoreProfileResult.isSuccess) {
            // Update with both Firebase Auth and Firestore profile data
            val profile = firestoreProfileResult.getOrNull()!!
            FirestoreUser.fromFirebaseUser(remoteUser).copy(
                name = profile.displayName.takeIf { it.isNotEmpty() } ?: remoteUser.displayName ?: "",
                profilePic = profile.photoUrl.takeIf { it.isNotEmpty() } ?: remoteUser.photoUrl?.toString()
            )
        } else {
            // Just use Firebase Auth data
            FirestoreUser.fromFirebaseUser(remoteUser)
        }

        // Save to Firestore
        usersCollection.document(firestoreUser.userId)
            .set(firestoreUser)
            .await()

        // If Firestore profile doesn't exist, create it to ensure consistency
        if (firestoreProfileResult.isFailure) {
            val profile = UserProfile(
                uid = remoteUser.uid,
                displayName = firestoreUser.name,
                email = firestoreUser.email,
                photoUrl = firestoreUser.profilePic ?: ""
            )
            userRepository.createUserProfile(profile)
        }

        Result.success(firestoreUser)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun isAuthenticated(): Boolean = firebaseAuth.currentUser != null

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

    private suspend fun <T> executeFirebaseAuthOperation(
        authOperation: suspend () -> T
    ): Result<T> = try {
        Result.success(authOperation())
    } catch (e: Exception) {
        Result.failure(Exception(FirebaseErrorMapper.getErrorMessage(e)))
    }
}
