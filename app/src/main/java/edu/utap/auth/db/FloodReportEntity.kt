package edu.utap.auth.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val timestamp: String,
    val latitude: Double,
    val longitude: Double,
    val photoUrl: String? = null,
    val severity: String, // 'low', 'medium', or 'high'
    val waterDepth: String? = null,
    val roadClosed: Boolean,
    val verified: Boolean
)