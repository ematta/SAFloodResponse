package edu.utap.flood.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import edu.utap.flood.model.DiscussionMessage
import edu.utap.flood.model.DiscussionThread
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
) : DiscussionRepositoryInterface {
    private val threadsCollection = firestore.collection("discussions").also {
        Log.d(TAG, "Using Firestore collection: discussions")
    }

    override suspend fun createThread(thread: DiscussionThread): Result<DiscussionThread> = try {
        threadsCollection.document(thread.threadId)
            .set(thread)
            .await()
        Result.success(thread)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getThreadById(threadId: String): Result<DiscussionThread> = try {
        val document = threadsCollection.document(threadId).get().await()
        if (document.exists()) {
            Result.success(document.toObject<DiscussionThread>()!!)
        } else {
            Result.failure(Exception("Thread not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateThread(thread: DiscussionThread): Result<DiscussionThread> = try {
        threadsCollection.document(thread.threadId)
            .set(thread)
            .await()
        Result.success(thread)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteThread(threadId: String): Result<Unit> = try {
        threadsCollection.document(threadId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addMessage(threadId: String, message: DiscussionMessage): Result<DiscussionMessage> = try {
        val threadRef = threadsCollection.document(threadId)
        val messagesCollection = threadRef.collection("messages")
        
        messagesCollection.document(message.messageId)
            .set(message)
            .await()
        
        // The message is added to the subcollection above.
        // No need to update the main thread document's message list here.
        Result.success(message)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateMessage(threadId: String, message: DiscussionMessage): Result<DiscussionMessage> = try {
        val messagesCollection = threadsCollection.document(threadId).collection("messages")
        messagesCollection.document(message.messageId)
            .set(message)
            .await()
        Result.success(message)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteMessage(threadId: String, messageId: String): Result<Unit> = try {
        threadsCollection.document(threadId)
            .collection("messages")
            .document(messageId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
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
                    document.toObject<DiscussionMessage>()
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
                    document.toObject<DiscussionThread>()
                } ?: emptyList()
                
                trySend(threads)
            }
        
        awaitClose { listener.remove() }
    }
}
