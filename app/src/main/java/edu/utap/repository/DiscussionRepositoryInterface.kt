package edu.utap.repository

import edu.utap.models.DiscussionMessage
import edu.utap.models.DiscussionThread
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining discussion thread and message operations.
 *
 * Implementations handle creating, updating, deleting, and observing discussion threads and messages.
 */
interface DiscussionRepositoryInterface {
    /**
     * Creates a new discussion thread.
     *
     * @param thread The thread to create.
     * @return Result containing the created thread or an error.
     */
    suspend fun createThread(thread: DiscussionThread): Result<DiscussionThread>

    /**
     * Retrieves a discussion thread by its ID.
     *
     * @param threadId The thread ID.
     * @return Result containing the thread or an error.
     */
    suspend fun getThreadById(threadId: String): Result<DiscussionThread>

    /**
     * Updates an existing discussion thread.
     *
     * @param thread The updated thread.
     * @return Result containing the updated thread or an error.
     */
    suspend fun updateThread(thread: DiscussionThread): Result<DiscussionThread>

    /**
     * Deletes a discussion thread by its ID.
     *
     * @param threadId The thread ID.
     * @return Result indicating success or error.
     */
    suspend fun deleteThread(threadId: String): Result<Unit>
    
    /**
     * Adds a message to a discussion thread.
     *
     * @param threadId The thread ID.
     * @param message The message to add.
     * @return Result containing the added message or an error.
     */
    suspend fun addMessage(threadId: String, message: DiscussionMessage): Result<DiscussionMessage>

    /**
     * Updates a message in a discussion thread.
     *
     * @param threadId The thread ID.
     * @param message The updated message.
     * @return Result containing the updated message or an error.
     */
    suspend fun updateMessage(threadId: String, message: DiscussionMessage): Result<DiscussionMessage>

    /**
     * Deletes a message from a discussion thread.
     *
     * @param threadId The thread ID.
     * @param messageId The message ID.
     * @return Result indicating success or error.
     */
    suspend fun deleteMessage(threadId: String, messageId: String): Result<Unit>
    
    /**
     * Observes messages in a discussion thread.
     *
     * @param threadId The thread ID.
     * @return A [Flow] emitting lists of messages.
     */
    fun observeThreadMessages(threadId: String): Flow<List<DiscussionMessage>>

    /**
     * Observes all discussion threads.
     *
     * @return A [Flow] emitting lists of threads.
     */
    fun observeAllThreads(): Flow<List<DiscussionThread>>
}
