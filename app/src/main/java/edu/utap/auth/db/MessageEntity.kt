package edu.utap.auth.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = DiscussionEntity::class,
            parentColumns = ["threadId"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["threadId"]),
        Index(value = ["userId"])
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