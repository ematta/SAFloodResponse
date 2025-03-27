package edu.utap.flood.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for flood report operations.
 *
 * This interface defines the database operations that can be performed on flood reports.
 * It provides methods to query, insert, update, and delete flood report data in the database.
 */
@Dao
interface FloodReportDao {
    /**
     * Gets all flood reports from the database as a Flow.
     *
     * @return A Flow emitting the list of all flood reports
     */
    @Query("SELECT * FROM flood_reports ORDER BY created_at DESC")
    fun getAllReports(): Flow<List<FloodReportEntity>>

    /**
     * Gets a specific flood report by its ID.
     *
     * @param reportId The unique identifier of the flood report to retrieve
     * @return The flood report with the specified ID, or null if not found
     */
    @Query("SELECT * FROM flood_reports WHERE report_id = :reportId")
    suspend fun getReportById(reportId: String): FloodReportEntity?

    /**
     * Gets flood reports within a specified radius of a location.
     *
     * @param latitude The latitude of the center point
     * @param longitude The longitude of the center point
     * @param radiusInMiles The radius in miles to search within
     * @return A list of flood reports within the specified radius
     */
    @Query(
        """
        SELECT * FROM flood_reports 
        WHERE (
            3959 * acos(
                cos(radians(:latitude)) * cos(radians(latitude)) * 
                cos(radians(longitude) - radians(:longitude)) + 
                sin(radians(:latitude)) * sin(radians(latitude))
            )
        ) <= :radiusInMiles
        ORDER BY created_at DESC
    """
    )
    suspend fun getReportsInRadius(
        latitude: Double,
        longitude: Double,
        radiusInMiles: Double
    ): List<FloodReportEntity>

    /**
     * Inserts or updates a flood report in the database.
     *
     * @param report The flood report to insert or update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: FloodReportEntity)

    /**
     * Updates an existing flood report in the database.
     *
     * @param report The flood report to update
     */
    @Update
    suspend fun updateReport(report: FloodReportEntity)

    /**
     * Deletes a flood report from the database.
     *
     * @param report The flood report to delete
     */
    @Delete
    suspend fun deleteReport(report: FloodReportEntity)
}
