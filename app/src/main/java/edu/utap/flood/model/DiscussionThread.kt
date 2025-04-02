package edu.utap.flood.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class DiscussionThread(
    @DocumentId val threadId: String = "",
    val reportId: String = "",
    val createdBy: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    @ServerTimestamp val timestamp: Timestamp? = null,
    val messages: List<DiscussionMessage> = emptyList()
)
