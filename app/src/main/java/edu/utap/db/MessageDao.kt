package edu.utap.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for message operations.
 *
 * This interface defines the database operations that can be performed on messages
 * within discussion threads. It provides methods to query and insert message data.
 * The methods are implemented by Room at compile time.
 */
@Dao
interface MessageDao {
    /**
     * Gets all messages for a specific discussion thread as a Flow, ordered by timestamp.
     *
     * Using Flow allows the UI to observe changes to the data and update automatically
     * when the underlying database changes. The messages are ordered chronologically
     * to display the conversation in the correct sequence.
     *
     * @param threadId The ID of the discussion thread to get messages for
     * @return A Flow emitting the list of messages for the specified thread, ordered by timestamp
     */
    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun getMessagesForThread(threadId: String): Flow<List<MessageEntity>>

    /**
     * Inserts or updates a message in the database.
     *
     * If a message with the same ID already exists, it will be replaced.
     *
     * @param message The message to insert or update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
}
