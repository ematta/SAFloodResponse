package edu.utap.auth.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a message in a discussion thread.
 * 
 * This entity stores individual messages that are part of discussion threads.
 * Each message belongs to a specific discussion thread and is authored by a user.
 * The entity maintains relationships with both the DiscussionEntity and UserEntity.
 * 
 * The CASCADE delete behavior ensures that when a discussion thread or user is deleted,
 * all associated messages are also deleted.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = DiscussionEntity::class,
            parentColumns = ["threadId"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE  // Delete messages when the parent thread is deleted
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE  // Delete messages when the author user is deleted
        )
    ],
    indices = [
        Index(value = ["threadId"]),  // Index for faster queries by threadId
        Index(value = ["userId"])     // Index for faster queries by userId
    ]
)
data class MessageEntity(
    @PrimaryKey
    val messageId: String,  // Unique identifier for the message
    val threadId: String,   // Foreign key to the discussion thread this message belongs to
    val userId: String,     // Foreign key to the user who authored this message
    val text: String,       // The content of the message
    val timestamp: String,  // When the message was created
    val upvotes: Int = 0    // Number of upvotes the message has received
)