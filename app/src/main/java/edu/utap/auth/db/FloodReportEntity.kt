package edu.utap.auth.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a flood report in the database.
 * 
 * This entity stores information about flood reports submitted by users,
 * including location data, severity assessment, and verification status.
 * It has a foreign key relationship with the UserEntity to track which user
 * submitted the report.
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
    val reportId: String,  // Unique identifier for the report
    val userId: String,    // Foreign key to the user who created the report
    val timestamp: String, // When the report was created
    val latitude: Double,  // Geographic coordinates of the flood
    val longitude: Double, // Geographic coordinates of the flood
    val photoUrl: String? = null, // Optional photo evidence of flooding
    val severity: String,  // Categorized as 'low', 'medium', or 'high'
    val waterDepth: String? = null, // Optional measurement of water depth
    val roadClosed: Boolean, // Indicates if the road is closed due to flooding
    val verified: Boolean   // Indicates if the report has been verified by authorities
)