package edu.utap.flood.repository

import edu.utap.flood.model.DiscussionMessage
import edu.utap.flood.model.DiscussionThread
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Repository implementation that delegates discussion operations to [FirestoreDiscussionRepository].
 *
 * This class acts as a wrapper to abstract away the Firestore-specific implementation,
 * allowing for easier testing and future replacement.
 *
 * @property firestoreDiscussionRepository The Firestore-backed repository implementation.
 */
class DiscussionRepository @Inject constructor(
    private val firestoreDiscussionRepository: FirestoreDiscussionRepository
) : DiscussionRepositoryInterface {
    override suspend fun createThread(thread: DiscussionThread): Result<DiscussionThread> {
        return firestoreDiscussionRepository.createThread(thread)
    }

    override suspend fun getThreadById(threadId: String): Result<DiscussionThread> {
        return firestoreDiscussionRepository.getThreadById(threadId)
    }

    override suspend fun updateThread(thread: DiscussionThread): Result<DiscussionThread> {
        return firestoreDiscussionRepository.updateThread(thread)
    }

    override suspend fun deleteThread(threadId: String): Result<Unit> {
        return firestoreDiscussionRepository.deleteThread(threadId)
    }

    override suspend fun addMessage(threadId: String, message: DiscussionMessage): Result<DiscussionMessage> {
        return firestoreDiscussionRepository.addMessage(threadId, message)
    }

    override suspend fun updateMessage(threadId: String, message: DiscussionMessage): Result<DiscussionMessage> {
        return firestoreDiscussionRepository.updateMessage(threadId, message)
    }

    override suspend fun deleteMessage(threadId: String, messageId: String): Result<Unit> {
        return firestoreDiscussionRepository.deleteMessage(threadId, messageId)
    }

    override fun observeThreadMessages(threadId: String): Flow<List<DiscussionMessage>> {
        return firestoreDiscussionRepository.observeThreadMessages(threadId)
    }

    override fun observeAllThreads(): Flow<List<DiscussionThread>> {
        return firestoreDiscussionRepository.observeAllThreads()
    }
}
