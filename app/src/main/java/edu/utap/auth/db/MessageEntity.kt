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
            onDelete = ForeignKey.CASCADE // Delete messages when the parent thread is deleted
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE // Delete messages when the author user is deleted
        )
    ],
    indices = [
        Index(value = ["threadId"]), // Index for faster queries by threadId
        Index(value = ["userId"]) // Index for faster queries by userId
    ]
)
data class MessageEntity(
    @PrimaryKey
    val messageId: String,
    val threadId: String,
    val userId: String,
    val text: String,
    val timestamp: String,
    val upvotes: Int = 0
)
