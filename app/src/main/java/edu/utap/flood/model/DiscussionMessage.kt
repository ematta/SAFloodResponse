package edu.utap.flood.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class DiscussionMessage(
    val messageId: String = "",
    val userId: String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null,
    val upvotes: Int = 0
)
