package edu.utap.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a discussion thread related to a flood report.
 *
 * @property threadId Unique identifier for the thread (Firestore document ID).
 * @property reportId ID of the associated flood report.
 * @property createdBy User ID of the thread creator.
 * @property category Category of the discussion (e.g., emergency, info).
 * @property tags List of tags associated with the thread.
 * @property timestamp Server-generated timestamp when the thread was created.
 * @property messages List of messages within the thread.
 * @property title Title of the discussion thread.
 * @property description Description or initial post content.
 * @property editTimestamp Server-generated timestamp of the last edit.
 */
data class DiscussionThread(
    @DocumentId val threadId: String = "",
    val reportId: String = "",
    val createdBy: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    @ServerTimestamp val timestamp: Timestamp? = null,
    val messages: List<DiscussionMessage> = emptyList(),

    @get:PropertyName("title")
    @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = "",

    @get:PropertyName("editTimestamp")
    @set:PropertyName("editTimestamp")
    @ServerTimestamp
    var editTimestamp: Timestamp? = null
)
