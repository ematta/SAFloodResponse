package edu.utap.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.models.FirestoreUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Interface defining authentication and user management operations.
 *
 * Implementations handle Firebase Authentication, Firestore user profiles,
 * and synchronization between them.
 */
interface AuthRepositoryInterface {
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
    suspend fun createUser(user: FirestoreUser): Result<FirestoreUser>
    suspend fun getUserByEmail(email: String): Result<FirestoreUser>
    suspend fun getUserById(userId: String): Result<FirestoreUser>
    suspend fun updateUser(user: FirestoreUser): Result<FirestoreUser>
    suspend fun syncUser(remoteUser: FirebaseUser): Result<FirestoreUser>
    suspend fun isAuthenticated(): Boolean
    fun observeUsers(): Flow<List<FirestoreUser>>
}

/**
 * Implementation of the [AuthRepositoryInterface] for handling user authentication and data management
 * using Firebase Authentication and Firestore.
 *
 * This class provides methods for:
 * - User registration
 * - User login
 * - Retrieving the current logged-in user
 * - User logout
 * - Password reset
 * - Creating, retrieving, updating and sync users in Firestore
 * - Checking if the user is authenticated
 * - Observing changes in the list of users
 *
 * @property firebaseAuth An instance of [FirebaseAuth] for handling user authentication.
 * @property firestore An instance of [FirebaseFirestore] for interacting with the Firestore database.
 */
class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepositoryInterface {

    private val usersCollection get() = firestore.collection("users")

    /**
     * Registers a new user with Firebase Authentication and stores user details in Firestore.
     *
     * This function performs the following steps:
     * 1. Creates a new user in Firebase Authentication using the provided email and password.
     * 2. Updates the user's profile with the provided name.
     * 3. Creates a corresponding document in Firestore with the user's details (userId, email, name, role).
     * 4. Returns a Result object indicating success or failure.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @param name The user's display name.
     * @param role The user's role (e.g., "admin", "user").
     * @return A [Result] object containing either:
     *         - [Result.success] with the [FirebaseUser] if registration is successful.
     *         - [Result.failure] with an [Exception] if registration fails. The exception will contain details about the failure.
     *
     * @throws Exception if any of the Firebase operations fail (e.g., user creation, profile update, Firestore write).
     */
    override suspend fun registerUser(
        email: String,
        password: String,
        name: String,
        role: String
    ): Result<FirebaseUser> {
        return try {
            val authResult = Tasks.await(
                firebaseAuth.createUserWithEmailAndPassword(email, password)
            )
            val user = authResult.user ?: return Result.failure(Exception("User creation failed"))
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            Tasks.await(user.updateProfile(profileUpdates))

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

    /**
     * Logs in a user with the provided email and password.
     *
     * This function attempts to authenticate a user with Firebase Authentication using their
     * email and password. It uses a suspending function and awaits the result of the
     * asynchronous Firebase operation.
     *
     * @param email The email address of the user.
     * @param password The password of the user.
     * @return A [Result] object representing the outcome of the login attempt.
     *         - [Result.Success] containing the [FirebaseUser] object if the login was successful.
     *         - [Result.Failure] containing an [Exception] if the login failed.
     *           Possible exceptions include:
     *              - [com.google.firebase.auth.FirebaseAuthInvalidCredentialsException]: If the email/password is invalid.
     *              - [com.google.firebase.auth.FirebaseAuthInvalidUserException]: If no user is found with the specified email.
     *              - [com.google.firebase.FirebaseNetworkException]: If there is a network issue.
     *              -  Any other `Exception` that might occur during the authentication process.
     *           - If authentication succeeds but `authResult.user` is `null`, it returns [Result.Failure] with a generic "Authentication failed" exception.
     *
     * @throws Exception If there is an error during the authentication process.
     */
    override suspend fun loginUser(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = Tasks.await(
                firebaseAuth.signInWithEmailAndPassword(email, password)
            )
            val user = authResult.user ?: return Result.failure(Exception("Authentication failed"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves the currently signed-in user from Firebase Authentication.
     *
     * This function provides a convenient way to access the `FirebaseUser` object
     * representing the user who is currently authenticated with Firebase. If no user
     * is signed in, it returns `null`.
     *
     * Note: This is a suspending function, intended to be called within a coroutine or
     * another suspending function. It interacts with the Firebase Authentication service,
     * which may involve network operations.
     *
     * @return The `FirebaseUser` object representing the currently signed-in user, or
     *         `null` if no user is signed in.
     */
    override suspend fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    /**
     * Logs out the currently authenticated user.
     *
     * This function signs out the user from Firebase Authentication. After calling this function,
     * the user will no longer be authenticated, and any subsequent requests requiring authentication
     * will fail.
     *
     * Note: This is a suspend function and should be called within a coroutine scope.
     *
     * @throws Exception If there is an error during the sign-out process (unlikely with Firebase SDK).
     */
    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    /**
     * Sends a password reset email to the specified email address.
     *
     * This function uses Firebase Authentication to send a password reset email.
     * It attempts to send the email and returns a Result object indicating
     * success or failure.
     *
     * @param email The email address to which the password reset email should be sent.
     * @return A Result object:
     *         - Result.success(Unit) if the password reset email was sent successfully.
     *         - Result.failure(e) if an error occurred during the process. The Exception 'e'
     *           will contain details about the failure.
     * @throws Exception if there is an issue with Firebase Authentication or network connectivity. This exception is caught and returned within Result.failure.
     */
    override suspend fun resetPassword(email: String): Result<Unit> = try {
        Tasks.await(
            firebaseAuth.sendPasswordResetEmail(email)
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Creates a new user document in the Firestore users collection.
     *
     * This function attempts to create a new document in the Firestore "users" collection,
     * using the provided [FirestoreUser]'s `userId` as the document ID.
     *
     * @param user The [FirestoreUser] object containing the user data to be stored. The `userId` field
     *             of this object will be used as the document ID.
     * @return A [Result] object indicating the success or failure of the operation.
     *         - [Result.success] containing the original [FirestoreUser] object if the creation was successful.
     *         - [Result.failure] containing an [Exception] if the creation failed.
     * @throws Exception if any error occurs during the Firestore write operation, such as network issues,
     *         invalid data, or Firestore-related errors. This exception will be wrapped in the [Result.failure].
     */
    override suspend fun createUser(user: FirestoreUser): Result<FirestoreUser> = try {
        Tasks.await(
            usersCollection.document(user.userId).set(user)
        )
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Retrieves a FirestoreUser from the database based on the provided email.
     *
     * This function queries the "users" collection in Firestore for a document
     * where the "email" field matches the given email address.
     *
     * @param email The email address of the user to retrieve.
     * @return A [Result] object that either contains the [FirestoreUser] if found,
     *         or a [Result.failure] with an exception if:
     *         - No user is found with the given email.
     *         - There's an error parsing the document into a [FirestoreUser] object.
     *         - An exception occurs during the Firestore query.
     *
     * @throws Exception If any error occurs during the Firestore operation, such as:
     *                   - Network issues
     *                   - Firestore permission errors
     *                   - Other exceptions thrown by the underlying Firestore SDK.
     *
     * @sample
     *  ```kotlin
     *   // Example usage within a coroutine
     *   val result = userRepository.getUserByEmail("test@example.com")
     *   result.onSuccess { firestoreUser ->
     *       // Handle the successful retrieval of the user.
     *       println("User found: ${firestoreUser.name}")
     *   }.onFailure { exception ->
     *       // Handle the error
     *       println("Error getting user: ${exception.message}")
     *   }
     *  ```
     */
    override suspend fun getUserByEmail(email: String): Result<FirestoreUser> {
        return try {
            val querySnapshot = Tasks.await(
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

    /**
     * Retrieves a user from Firestore by their unique user ID.
     *
     * This function asynchronously fetches a user document from the Firestore 'users' collection
     * using the provided [userId]. It attempts to retrieve the document, and if successful,
     * it deserializes the document into a [FirestoreUser] object.
     *
     * @param userId The unique identifier of the user to retrieve.
     * @return A [Result] object representing the outcome of the operation.
     *         - [Result.success] containing the [FirestoreUser] object if the user is found and successfully parsed.
     *         - [Result.failure] containing an [Exception] if:
     *             - The user with the specified [userId] does not exist in the Firestore collection.
     *             - There is an error during the Firestore document retrieval process.
     *             - There is an error during the deserialization of the document into a [FirestoreUser] object.
     *
     * @throws Exception if there's an underlying issue with the Firestore operations or data parsing.
     */
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

    /**
     * Updates an existing user in Firestore.
     *
     * This function attempts to update the document corresponding to the given user's ID in the
     * Firestore "users" collection. It uses the provided [FirestoreUser] object to overwrite the
     * existing document's data.
     *
     * This is a suspending function, meaning it should be called within a coroutine scope. It leverages
     * the `com.google.android.gms.tasks.Tasks.await` method to handle the asynchronous nature of
     * Firestore operations and ensures the function only returns once the update operation is complete.
     *
     * @param user The [FirestoreUser] object containing the updated user information. The `userId`
     *             field of this object is used to identify the document to update in Firestore.
     * @return A [Result] object indicating the outcome of the operation:
     *         - [Result.success] with the updated [FirestoreUser] if the update was successful.
     *         - [Result.failure] with an [Exception] describing the error if the update failed.
     *
     * @throws Exception If any error occurs during the Firestore operation, it will be caught and
     *                   wrapped in a `Result.failure` object. Possible errors include network issues,
     *                   permission problems, or invalid data.
     */
    override suspend fun updateUser(user: FirestoreUser): Result<FirestoreUser> = try {
        com.google.android.gms.tasks.Tasks.await(
            usersCollection.document(user.userId).set(user)
        )
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Synchronizes a user from the remote Firebase authentication system with the local Firestore database.
     *
     * This function attempts to retrieve a user from Firestore using the provided FirebaseUser's UID.
     * If the user exists in Firestore, it returns a successful result containing the FirestoreUser.
     * If the user does not exist, it creates a new FirestoreUser based on the FirebaseUser's information,
     * stores it in Firestore, and returns the result of that operation.
     *
     * @param remoteUser The FirebaseUser object representing the user from the remote authentication system.
     * @return A Result object encapsulating either:
     *         - A successful outcome containing the FirestoreUser found or created.
     *         - A failure outcome with the exception encountered during the process (either fetching or creating the user).
     *
     * @throws Exception If any error occurs during the retrieval or creation of the user in Firestore. The specific type of
     *         exception will depend on the underlying Firestore operations.
     */
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

    /**
     * Checks if a user is currently authenticated.
     *
     * This function determines whether a user is signed in by checking the current user
     * status of the Firebase Authentication instance.
     *
     * @return `true` if a user is currently signed in, `false` otherwise.
     */
    override suspend fun isAuthenticated(): Boolean = firebaseAuth.currentUser != null

    /**
     * Observes changes to the users collection in Firestore and emits a list of [FirestoreUser] objects.
     *
     * This function sets up a snapshot listener on the `usersCollection`.  Whenever the collection
     * changes (e.g., a user is added, updated, or deleted), the listener is triggered.
     * The function then maps the snapshot documents to [FirestoreUser] objects and emits them as a list
     * through a [Flow].
     *
     * **Error Handling:**
     * If any error occurs during the snapshot listening process (e.g., network error, permissions issue),
     * the error is propagated through the [Flow] by closing the channel with the error.
     *
     * **Cleanup:**
     * The snapshot listener is automatically removed when the [Flow] is closed (e.g., when the collector
     * cancels its subscription or the coroutine scope is cancelled). This is handled by the `awaitClose` block.
     *
     * **Emitted Values:**
     * The [Flow] emits a `List<FirestoreUser>` on every change to the users collection.
     *
     * **Thread Safety:**
     * This function uses `callbackFlow` which is designed to safely bridge callback-based APIs to [Flow].
     * It handles the proper context switching and cancellation, ensuring that the code within the
     * `addSnapshotListener` callback runs in the correct coroutine context.
     *
     * @return A [Flow] that emits a list of [FirestoreUser] objects representing the current state of the users collection.
     */
    override fun observeUsers(): Flow<List<FirestoreUser>> = callbackFlow {
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
