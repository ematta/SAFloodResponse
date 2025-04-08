package edu.utap.flood.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data class representing a message within a discussion thread.
 *
 * @property messageId Unique identifier for the message.
 * @property userId ID of the user who posted the message.
 * @property text Content of the message.
 * @property timestamp Server-generated timestamp when the message was created.
 * @property upvotes Number of upvotes the message has received.
 */
data class DiscussionMessage(
    val messageId: String = "",
    val userId: String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null,
    val upvotes: Int = 0
)
