package edu.utap.repository

import com.google.firebase.FirebaseException
import com.google.firebase.firestore.DocumentSnapshot
import edu.utap.utils.FirebaseErrorMapper

/**
 * Abstract base class for repositories that provides common functionality for handling
 * network and Firestore operations.
 *
 * This class offers:
 * - `safeFirestoreCall`: A utility function to safely execute Firestore operations and handle potential
 *   FirebaseExceptions or other Throwables, wrapping the result in a `Result` object.
 * - `safeNetworkCall`: A utility function to safely execute network operations and handle potential
 *   FirebaseExceptions (in case the network call interacts with Firebase) or other Throwables,
 *   wrapping the result in a `Result` object.
 * - `mapFirebaseError`: A function to map FirebaseExceptions to user-friendly error messages using
 *    a `FirebaseErrorMapper`.
 * - `toDomainObject`: An extension function to convert a `DocumentSnapshot` from Firestore to a
 *   domain object of type `T`.
 *
 * Subclasses should extend this class to take advantage of these utilities when
 * interacting with Firestore or performing network requests.
 */
abstract class BaseRepository {

    /**
     * Executes a Firestore operation within a safe context, handling potential FirebaseExceptions and other Throwables.
     *
     * This function provides a robust way to interact with Firestore by wrapping operations in a try-catch block.
     * It attempts to execute the provided [operation]. If successful, it returns a [Result.success] containing the operation's result.
     * If a [FirebaseException] is thrown, it maps the exception to a user-friendly error message using [mapFirebaseError] and returns a [Result.failure] with an Exception containing this message.
     * If any other [Throwable] is thrown, it returns a [Result.failure] with that Throwable.
     *
     * @param operation The suspend function representing the Firestore operation to execute.
     * @return A [Result] object indicating the success or failure of the operation.
     *   - [Result.success]: Contains the result of the [operation] if successful.
     *   - [Result.failure]: Contains an [Exception] with a user-friendly message if a [FirebaseException] occurred, or the original [Throwable] if any other exception occurred.
     */
    protected suspend fun <T> safeFirestoreCall(operation: suspend () -> T): Result<T> = try {
        val result = operation()
        Result.success(result)
    } catch (e: FirebaseException) {
        val message = mapFirebaseError(e)
        Result.failure(Exception(message))
    } catch (e: Throwable) {
        Result.failure(e)
    }

    /**
     * Executes a network operation safely, handling potential exceptions and returning a Result.
     *
     * This function is designed to wrap network calls (or any potentially failing operation) within a try-catch block.
     * It specifically handles [FirebaseException] and maps them to more user-friendly error messages.
     * All other [Throwable]s are caught and returned as failures as well.
     *
     * @param operation The suspend function representing the network operation to execute.
     * @return A [Result] object that either:
     *   - Holds the successful result of the operation in [Result.success].
     *   - Holds an [Exception] representing the failure that occurred in [Result.failure]. The exception will either contain the mapped Firebase error message or the original Throwable's information.
     *
     * @throws No exceptions are thrown directly. Instead, failures are wrapped within the [Result] object.
     */
    protected suspend fun <T> safeNetworkCall(operation: suspend () -> T): Result<T> = try {
        val result = operation()
        Result.success(result)
    } catch (e: FirebaseException) {
        val message = mapFirebaseError(e)
        Result.failure(Exception(message))
    } catch (e: Throwable) {
        Result.failure(e)
    }

    /**
     * Maps a Firebase exception to a user-friendly error message.
     *
     * This function takes a Throwable, which is typically a FirebaseException,
     * and uses the FirebaseErrorMapper to convert it into a human-readable error string.
     * This allows for a consistent and centralized way to handle and display errors
     * originating from Firebase services.
     *
     * @param exception The Throwable (typically a FirebaseException) that needs to be mapped.
     * @return A user-friendly error message string derived from the exception.
     * @see FirebaseErrorMapper
     */
    protected fun mapFirebaseError(exception: Throwable): String =
        FirebaseErrorMapper.getErrorMessage(exception)

    /**
     * Converts a Firestore [DocumentSnapshot] to a domain object of type [T].
     *
     * This function uses Firestore's built-in `toObject` method to deserialize the document data
     * into an object of the specified type [T].  It leverages Kotlin's reified type parameters
     * to allow the type to be determined at runtime.
     *
     * **Important Considerations:**
     *
     * *   **Data Structure:** The structure of the Firestore document must precisely match the
     *     properties of the domain object class [T].  Firestore field names will be mapped to
     *     properties of [T] by name (case-sensitive).
     * *   **Data Types:** The data types stored in Firestore should be compatible with the
     *     corresponding property types in [T]. Firestore's type system is similar but not identical to
     *     Kotlin's. Refer to Firestore documentation for type mappings.
     * *   **Nullability:** If a field is missing in the document, the corresponding property
     *     in [T] will be set to `null` if it is a nullable type, otherwise, it will likely result in an exception during conversion.
     * * **Default Constructor:** The class [T] needs to have a no-argument public constructor. If it is not provided, the function will fail.
     * *   **Performance:** While convenient, this method relies on reflection. For performance-critical
     *     applications, consider creating manual mapping functions.
     *
     * @param T The type of the domain object to convert to. This is a reified type parameter,
     *          meaning the actual type is available at runtime.
     * @return The domain object of type [T] created from the document data, or `null` if the
     *         document data could not be converted to [T] (e.g., if the document doesn't exist or
     *         if the data doesn't match the expected structure) or if the document itself is null.
     *
     * @throws com.google.firebase.firestore.FirebaseFirestoreException if there is an issue accessing the document
     * @throws RuntimeException if the class T does not have a default constructor
     *
     * @see com.google.firebase.firestore.DocumentSnapshot.toObject
     */
    protected inline fun <reified T> DocumentSnapshot.toDomainObject(): T? =
        this.toObject(T::class.java)
}
