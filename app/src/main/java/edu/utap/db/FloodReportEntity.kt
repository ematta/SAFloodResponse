package edu.utap.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing a flood report in the database.
 *
 * This entity stores comprehensive flood report information including location, description,
 * photos, status, timestamps, and verification details. It serves as the primary flood report record
 * in the local database and corresponds to Firestore flood reports.
 *
 * It has a foreign key relationship with the UserEntity to track which user submitted the report,
 * ensuring referential integrity in the database.
 */
@Entity(
    tableName = "flood_reports",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"])
    ]
)
data class FloodReportEntity(
    @PrimaryKey
    val reportId: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val photoUrls: List<String>,
    val photoUrl: String? = null, // Single photo URL from auth version
    val status: String,
    val severity: String? = null,
    val waterDepth: String? = null,
    val roadClosed: Boolean = false,
    val verified: Boolean = false,
    val createdAt: Date,
    val updatedAt: Date,
    val timestamp: String? = null, // String timestamp from auth version
    val isManualLocation: Boolean = false,
    val confirmedCount: Int = 0,
    val deniedCount: Int = 0
)
