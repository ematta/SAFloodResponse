package edu.utap.flood.repository

import edu.utap.flood.model.DiscussionMessage
import edu.utap.flood.model.DiscussionThread
import kotlinx.coroutines.flow.Flow

interface DiscussionRepositoryInterface {
    suspend fun createThread(thread: DiscussionThread): Result<DiscussionThread>
    suspend fun getThreadById(threadId: String): Result<DiscussionThread>
    suspend fun updateThread(thread: DiscussionThread): Result<DiscussionThread>
    suspend fun deleteThread(threadId: String): Result<Unit>
    
    suspend fun addMessage(threadId: String, message: DiscussionMessage): Result<DiscussionMessage>
    suspend fun updateMessage(threadId: String, message: DiscussionMessage): Result<DiscussionMessage>
    suspend fun deleteMessage(threadId: String, messageId: String): Result<Unit>
    
    fun observeThreadMessages(threadId: String): Flow<List<DiscussionMessage>>
    fun observeAllThreads(): Flow<List<DiscussionThread>>
}
