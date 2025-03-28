package edu.utap.flood.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing a flood report in the local database.
 *
 * This entity stores flood report information including location, description,
 * photos, status, and timestamps. It serves as the primary flood report record
 * in the local database and corresponds to Firestore flood reports.
 */
@Entity(tableName = "flood_reports")
data class FloodReportEntity(
    @PrimaryKey
    val reportId: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val photoUrls: List<String>,
    val status: String,
    val createdAt: Date,
    val updatedAt: Date,
    val isManualLocation: Boolean = false,
    val confirmedCount: Int = 0,
    val deniedCount: Int = 0
)
