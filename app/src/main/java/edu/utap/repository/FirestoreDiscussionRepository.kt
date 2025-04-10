package edu.utap.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.models.DiscussionMessage
import edu.utap.models.DiscussionThread
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "FirestoreDiscussionRepo"

/**
 * Firestore-backed implementation of [DiscussionRepositoryInterface].
 *
 * This repository manages discussion threads and messages stored in Firestore,
 * including real-time updates via snapshot listeners.
 *
 * @property firestore The Firestore database instance.
 */
class FirestoreDiscussionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : BaseRepository(), DiscussionRepositoryInterface {
    private val threadsCollection = firestore.collection("discussions").also {
        Log.d(TAG, "Using Firestore collection: discussions")
    }

    override suspend fun createThread(thread: DiscussionThread): Result<DiscussionThread> = safeFirestoreCall {
        threadsCollection.document(thread.threadId)
            .set(thread)
            .await()
        thread
    }

    override suspend fun getThreadById(threadId: String): Result<DiscussionThread> = safeFirestoreCall {
        val document = threadsCollection.document(threadId).get().await()
        if (document.exists()) {
            document.toDomainObject<DiscussionThread>() ?: throw Exception("Failed to parse thread")
        } else {
            throw Exception("Thread not found")
        }
    }

    override suspend fun updateThread(thread: DiscussionThread): Result<DiscussionThread> = safeFirestoreCall {
        threadsCollection.document(thread.threadId)
            .set(thread)
            .await()
        thread
    }

    override suspend fun deleteThread(threadId: String): Result<Unit> = safeFirestoreCall {
        threadsCollection.document(threadId).delete().await()
        Unit
    }

    override suspend fun addMessage(threadId: String, message: DiscussionMessage): Result<DiscussionMessage> = safeFirestoreCall {
        val threadRef = threadsCollection.document(threadId)
        val messagesCollection = threadRef.collection("messages")
        
        messagesCollection.document(message.messageId)
            .set(message)
            .await()
        
        message
    }

    override suspend fun updateMessage(threadId: String, message: DiscussionMessage): Result<DiscussionMessage> = safeFirestoreCall {
        val messagesCollection = threadsCollection.document(threadId).collection("messages")
        messagesCollection.document(message.messageId)
            .set(message)
            .await()
        message
    }

    override suspend fun deleteMessage(threadId: String, messageId: String): Result<Unit> = safeFirestoreCall {
        threadsCollection.document(threadId)
            .collection("messages")
            .document(messageId)
            .delete()
            .await()
        Unit
    }

    override fun observeThreadMessages(threadId: String): Flow<List<DiscussionMessage>> = callbackFlow {
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
