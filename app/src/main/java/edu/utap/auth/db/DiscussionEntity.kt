package edu.utap.auth.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "discussions",
    foreignKeys = [
        ForeignKey(
            entity = FloodReportEntity::class,
            parentColumns = ["reportId"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["createdBy"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["reportId"]),
        Index(value = ["createdBy"])
    ]
)
data class DiscussionEntity(
    @PrimaryKey
    val threadId: String,
    val reportId: String,
    val createdBy: String,
    val timestamp: String
)