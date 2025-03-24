package edu.utap.auth.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for flood report operations.
 * 
 * This interface defines the database operations that can be performed on flood reports.
 * It provides methods to query, insert, and retrieve flood report data from the database.
 * The methods are implemented by Room at compile time.
 */
@Dao
interface FloodReportDao {
    /**
     * Gets all flood reports from the database as a Flow.
     * 
     * Using Flow allows the UI to observe changes to the data and update automatically
     * when the underlying database changes.
     * 
     * @return A Flow emitting the list of all flood reports
     */
    @Query("SELECT * FROM flood_reports")
    fun getAllReports(): Flow<List<FloodReportEntity>>
    
    /**
     * Gets a specific flood report by its ID.
     * 
     * @param reportId The unique identifier of the report to retrieve
     * @return The flood report with the specified ID, or null if not found
     */
    @Query("SELECT * FROM flood_reports WHERE reportId = :reportId")
    suspend fun getReportById(reportId: String): FloodReportEntity?
    
    /**
     * Inserts or updates a flood report in the database.
     * 
     * If a report with the same ID already exists, it will be replaced.
     * 
     * @param report The flood report to insert or update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: FloodReportEntity)
}