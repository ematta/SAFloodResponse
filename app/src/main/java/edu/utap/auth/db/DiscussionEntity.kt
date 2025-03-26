package edu.utap.auth.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a discussion thread in the database.
 *
 * This entity represents a discussion thread related to a specific flood report.
 * It maintains relationships with both the FloodReportEntity (the report being discussed)
 * and the UserEntity (the user who created the discussion thread).
 *
 * The CASCADE delete behavior ensures that when a flood report or user is deleted,
 * all associated discussion threads are also deleted.
 */
@Entity(
    tableName = "discussions",
    foreignKeys = [
        ForeignKey(
            entity = FloodReportEntity::class,
            parentColumns = ["reportId"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE // Delete discussions when the related report is deleted
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["createdBy"],
            onDelete = ForeignKey.CASCADE // Delete discussions when the creator user is deleted
        )
    ],
    indices = [
        Index(value = ["reportId"]), // Index for faster queries by reportId
        Index(value = ["createdBy"]) // Index for faster queries by creator
    ]
)
data class DiscussionEntity(
    @PrimaryKey
    val threadId: String,
    val reportId: String,
    val createdBy: String,
    val timestamp: String
)
