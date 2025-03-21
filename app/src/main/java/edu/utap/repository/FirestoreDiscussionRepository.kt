package edu.utap.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.models.DiscussionMessage
import edu.utap.models.DiscussionThread
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreDiscussionRepo"

/**
 * Interface for interacting with discussion threads and messages in a repository.
 *
 * This interface defines the contract for managing discussion data, including creating,
 * retrieving, updating, and deleting threads and messages. It also provides the ability
 * to observe changes to threads and messages in real-time.
 */
interface DiscussionRepositoryInterface {
    suspend fun createThread(thread: DiscussionThread): Result<DiscussionThread>
    suspend fun getThreadById(threadId: String): Result<DiscussionThread>
    suspend fun updateThread(thread: DiscussionThread): Result<DiscussionThread>
    suspend fun deleteThread(threadId: String): Result<Unit>
    suspend fun addMessage(threadId: String, message: DiscussionMessage): Result<DiscussionMessage>
    suspend fun updateMessage(
        threadId: String,
        message: DiscussionMessage
    ): Result<DiscussionMessage>
    suspend fun deleteMessage(threadId: String, messageId: String): Result<Unit>
    fun observeThreadMessages(threadId: String): Flow<List<DiscussionMessage>>
    fun observeAllThreads(): Flow<List<DiscussionThread>>
}

/**
 * Firestore-backed implementation of [DiscussionRepositoryInterface].
 *
 * This repository manages discussion threads and messages stored in Firestore,
 * including real-time updates via snapshot listeners.
 *
 * @property firestore The Firestore database instance.
 */
class FirestoreDiscussionRepository @Inject constructor(private val firestore: FirebaseFirestore) :
    BaseRepository(),
    DiscussionRepositoryInterface {
    private val threadsCollection = firestore.collection("discussions").also {
        Log.d(TAG, "Using Firestore collection: discussions")
    }

    /**
     * Creates a new discussion thread in the Firestore database.
     *
     * This function attempts to add a new `DiscussionThread` to the `threadsCollection` in Firestore.
     * It uses the `threadId` of the provided `DiscussionThread` as the document ID.
     *
     * @param thread The `DiscussionThread` object to be created. This object should have a unique `threadId`.
     * @return A `Result` object that either:
     *         - Contains the successfully created `DiscussionThread` if the operation was successful.
     *         - Contains a `Throwable` if the operation failed, which can be inspected for the specific error.
     *
     * @throws Exception if any error occurs during the Firestore operation. This error will be encapsulated within the `Result.failure` case.
     */
    override suspend fun createThread(thread: DiscussionThread): Result<DiscussionThread> =
        safeFirestoreCall {
            threadsCollection.document(thread.threadId)
                .set(thread)
                .await()
            thread
        }

    /**
     * Retrieves a discussion thread by its unique ID.
     *
     * This function fetches a thread from the Firestore "threads" collection using the provided `threadId`.
     * It handles potential errors during the database operation and returns a `Result` object indicating success or failure.
     *
     * @param threadId The unique identifier of the discussion thread to retrieve.
     * @return A `Result` object containing:
     *         - `Result.Success<DiscussionThread>`: If the thread is found and successfully parsed, it contains the `DiscussionThread` object.
     *         - `Result.Failure<Exception>`: If an error occurs during the process, it contains an `Exception` indicating the failure reason.
     *           Possible failure reasons include:
     *           - "Thread not found": If a document with the specified `threadId` does not exist in the "threads" collection.
     *           - "Failed to parse thread": If the document exists but cannot be parsed into a `DiscussionThread` object.
     *           - Other exceptions: Any other error that might occur during the Firestore operation.
     *
     * @throws Exception if there is any issue with the firestore call or if thread is not found.
     */
    override suspend fun getThreadById(threadId: String): Result<DiscussionThread> =
        safeFirestoreCall {
            val document = threadsCollection.document(threadId).get().await()
            if (document.exists()) {
                document.toDomainObject<DiscussionThread>()
                    ?: throw Exception("Failed to parse thread")
            } else {
                throw Exception("Thread not found")
            }
        }

    /**
     * Updates an existing discussion thread in Firestore.
     *
     * This function attempts to update the specified `DiscussionThread` in the Firestore database.
     * It uses the `threadId` of the provided `thread` object to identify the document to update.
     * If the document does not exist, it will be created.
     *
     * This function is a suspend function and should be called within a coroutine.
     * It utilizes a `safeFirestoreCall` (assumed to be a wrapper for error handling) to handle
     * potential Firestore exceptions and return a `Result` object.
     *
     * @param thread The `DiscussionThread` object containing the updated information. The `threadId`
     *               must be set to identify the thread to be updated.
     * @return A `Result` object containing either:
     *         - `Success(DiscussionThread)`: The updated `DiscussionThread` object if the update was successful.
     *         - `Failure(Exception)`: An exception indicating the type of error encountered during the operation.
     *                              This could be a FirestoreException, IOException, or other potential error.
     */
    override suspend fun updateThread(thread: DiscussionThread): Result<DiscussionThread> =
        safeFirestoreCall {
            threadsCollection.document(thread.threadId)
                .set(thread)
                .await()
            thread
        }

    /**
     * Deletes a thread from the Firestore database.
     *
     * This function asynchronously deletes the document associated with the given [threadId]
     * from the "threads" collection in Firestore. It uses a safe Firestore call wrapper
     * ([safeFirestoreCall]) to handle potential exceptions during the database operation.
     *
     * @param threadId The ID of the thread to be deleted. This corresponds to the document ID
     *                 within the "threads" collection in Firestore.
     * @return A [Result] object indicating the outcome of the operation.
     *         - [Result.success] if the thread was successfully deleted. The success value is [Unit].
     *         - [Result.failure] if an error occurred during the deletion process. The failure
     *           will contain the exception that caused the operation to fail.
     *
     * @throws Exception if there is an issue with the Firestore connection or an error during
     *                   the deletion operation. This exception will be wrapped within the
     *                   [Result.failure] if caught by the `safeFirestoreCall` function.
     *
     * Example usage:
     * ```kotlin
     * val firestoreService = FirestoreService(SafeFirestoreCallerImplementation()) //Replace SafeFirestoreCallerImplementation with your implementation
     * val result = firestoreService.deleteThread("someThreadId")
     * result.onSuccess {
     *     println("Thread deleted successfully!")
     * }
     * result.onFailure {
     *     println("Failed to delete thread: ${it.message}")
     * }
     * ```
     */
    override suspend fun deleteThread(threadId: String): Result<Unit> = safeFirestoreCall {
        threadsCollection.document(threadId).delete().await()
        Unit
    }

    /**
     * Adds a new message to a specific discussion thread in Firestore.
     *
     * This function attempts to add a `DiscussionMessage` to the "messages" subcollection
     * of the specified thread. It uses the message's `messageId` as the document ID within
     * the subcollection. The operation is performed asynchronously.
     *
     * @param threadId The ID of the discussion thread to which the message should be added.
     * @param message The `DiscussionMessage` object containing the message details to be added.
     * @return A `Result` object that encapsulates the outcome of the operation.
     *   - `Result.Success(DiscussionMessage)`: If the message was successfully added, the original
     *     `DiscussionMessage` object is returned.
     *   - `Result.Failure(Exception)`: If an error occurred during the operation (e.g., network
     *     issues, Firestore errors), a `Result.Failure` containing the exception is returned.
     *
     * @throws Exception If any error happens during the Firestore operation
     */
    override suspend fun addMessage(
        threadId: String,
        message: DiscussionMessage
    ): Result<DiscussionMessage> = safeFirestoreCall {
        val threadRef = threadsCollection.document(threadId)
        val messagesCollection = threadRef.collection("messages")

        messagesCollection.document(message.messageId)
            .set(message)
            .await()

        message
    }

    /**
     * Updates an existing message within a specified discussion thread in Firestore.
     *
     * This function attempts to update the message with the given `message` in the "messages"
     * subcollection of the thread identified by `threadId`. If the message does not exist,
     * it will be created.
     *
     * @param threadId The ID of the discussion thread where the message resides.
     * @param message The [DiscussionMessage] object containing the updated message data. The
     *                `messageId` within this object is used to identify the specific message to
     *                update.
     * @return A [Result] object.
     *         - If the update is successful, it contains the updated [DiscussionMessage] object.
     *         - If the update fails (e.g., due to network issues or Firestore errors), it contains a
     *           [Throwable] representing the error.
     *
     * @throws Exception if there's an error during the Firestore operation. This will be caught
     *                   by [safeFirestoreCall] and returned as a failure result.
     */
    override suspend fun updateMessage(
        threadId: String,
        message: DiscussionMessage
    ): Result<DiscussionMessage> = safeFirestoreCall {
        val messagesCollection = threadsCollection.document(threadId).collection("messages")
        messagesCollection.document(message.messageId)
            .set(message)
            .await()
        message
    }

    /**
     * Deletes a message from a specific thread in Firestore.
     *
     * This function asynchronously deletes a message identified by `messageId` from the thread identified by `threadId`.
     * It utilizes the `safeFirestoreCall` higher-order function to handle potential Firestore exceptions and
     * returns a `Result` object indicating success or failure.
     *
     * @param threadId The unique identifier of the thread containing the message to be deleted.
     * @param messageId The unique identifier of the message to be deleted within the specified thread.
     * @return A `Result<Unit>` object.
     *   - `Result.success(Unit)` if the message was successfully deleted.
     *   - `Result.failure(exception)` if an error occurred during the deletion process. The exception will contain
     *     details about the failure, such as a network error or a Firestore permission issue.
     *
     * @throws Exception Any exception thrown by the Firestore SDK during the deletion process. These exceptions are caught
     *  and encapsulated in the Result.failure.
     *
     * Example Usage:
     * ```kotlin
     * val result = deleteMessage("thread123", "message456")
     * result.onSuccess {
     *     println("Message deleted successfully")
     * }
     * result.onFailure { exception ->
     *     println("Failed to delete message: ${exception.message}")
     *     // Handle the error, e.g., retry or display an error message to the user.
     * }
     * ```
     */
    override suspend fun deleteMessage(threadId: String, messageId: String): Result<Unit> =
        safeFirestoreCall {
            threadsCollection.document(threadId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()
            Unit
        }

    /**
     * Observes messages within a specific discussion thread in a real-time manner.
     *
     * This function establishes a listener on the Firestore collection of messages associated with the provided `threadId`.
     * It emits a stream of `List<DiscussionMessage>` whenever there are changes to the messages in the thread.
     * The messages are ordered by their timestamp in ascending order (oldest to newest).
     *
     * @param threadId The unique identifier of the discussion thread.
     * @return A [Flow] that emits a list of [DiscussionMessage] whenever there's a change in the thread's messages.
     *         The Flow will emit an empty list if the thread has no messages.
     *         The Flow will be closed with an error if the snapshot listener encounters an error.
     *
     * @throws Exception if any error occurs during the snapshot listener operation. The error will be propagated through the flow.
     *
     * **Usage:**
     * ```kotlin
     * lifecycleScope.launch {
     *     myRepository.observeThreadMessages("thread123").collect { messages ->
     *         // Handle the list of messages
     *         println("Received messages: $messages")
     *     }
     * }
     * ```
     *
     * **Error Handling:**
     * Any error encountered by the underlying Firestore snapshot listener, such as network issues or security rule violations,
     * will be caught and propagated as an exception to the [Flow].  The Flow will be closed after emitting the error.
     *
     * **Cancellation:**
     * The snapshot listener is automatically removed when the [Flow] is cancelled (e.g., when the coroutine scope is cancelled).
     * This ensures that resources are properly cleaned up.
     *
     * **Data Ordering:**
     * The messages emitted by the flow are ordered by their "timestamp" field in ascending order.
     *
     * **Empty List Handling:**
     * If a thread does not contain any message, an empty List will be emitted.
     */
    override fun observeThreadMessages(threadId: String): Flow<List<DiscussionMessage>> =
        callbackFlow {
            val messagesCollection = threadsCollection.document(threadId).collection("messages")
            val listener = messagesCollection
                .orderBy("timestamp")
                .addSnapshotListener { querySnapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val messages = querySnapshot?.documents?.mapNotNull { document ->
                        document.toDomainObject<DiscussionMessage>()
                    } ?: emptyList()

                    trySend(messages)
                }

            awaitClose { listener.remove() }
        }

    /**
     * Observes all discussion threads from the data source in real-time.
     *
     * This function establishes a listener on the underlying data source (e.g., Firestore collection)
     * to receive updates whenever the list of discussion threads changes. It emits a `Flow` of
     * `List<DiscussionThread>`, where each list represents the current state of all threads.
     *
     * The threads are sorted by their "timestamp" field in ascending order (oldest to newest).
     *
     * **Error Handling:**
     * - If any error occurs while listening to the data source, the flow will be closed with the
     *   encountered error.
     *
     * **Resource Management:**
     * - The listener is automatically removed when the flow is cancelled or collected.
     *
     * **Thread Safety:**
     * - This function is safe to be called from any thread. The `callbackFlow` builder handles
     *   coroutine context switching appropriately.
     *
     * @return A `Flow` that emits lists of `DiscussionThread` in real-time.
     * @throws Exception if any error occur with the listener.
     */
    override fun observeAllThreads(): Flow<List<DiscussionThread>> = callbackFlow {
        val listener = threadsCollection
            .orderBy("timestamp")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val threads = querySnapshot?.documents?.mapNotNull { document ->
                    document.toDomainObject<DiscussionThread>()
                } ?: emptyList()

                trySend(threads)
            }

        awaitClose { listener.remove() }
    }
}
