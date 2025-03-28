package edu.utap.flood.repository

import edu.utap.flood.model.FloodReport
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining operations for managing flood reports.
 *
 * This interface abstracts the flood report management operations,
 * allowing for different implementations (e.g., Firebase, local database)
 * and facilitating testing through dependency injection.
 */
interface FloodReportRepositoryInterface {
    /**
     * Creates a new flood report.
     *
     * @param report The flood report to create
     * @return Result containing the created report or an error
     */
    suspend fun createReport(report: FloodReport): Result<FloodReport>

    /**
     * Retrieves a flood report by ID.
     *
     * @param reportId The unique identifier of the flood report
     * @return Result containing the flood report or an error if not found
     */
    suspend fun getReportById(reportId: String): Result<FloodReport>

    /**
     * Gets flood reports within a specified radius of a location.
     *
     * @param latitude The latitude of the center point
     * @param longitude The longitude of the center point
     * @param radiusInMiles The radius in miles to search within
     * @return Result containing the list of flood reports or an error
     */
    suspend fun getReportsInRadius(
        latitude: Double,
        longitude: Double,
        radiusInMiles: Double
    ): Result<List<FloodReport>>

    /**
     * Updates an existing flood report.
     *
     * @param report The updated flood report
     * @return Result containing the updated report or an error
     */
    suspend fun updateReport(report: FloodReport): Result<FloodReport>

    /**
     * Deletes a flood report.
     *
     * @param reportId The unique identifier of the flood report to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteReport(reportId: String): Result<Unit>

    /**
     * Observes all flood reports as a Flow.
     *
     * @return Flow of all flood reports
     */
    fun observeAllReports(): Flow<List<FloodReport>>

    /**
     * Observes flood reports within a specified radius as a Flow.
     *
     * @param latitude The latitude of the center point
     * @param longitude The longitude of the center point
     * @param radiusInMiles The radius in miles to search within
     * @return Flow of flood reports within the specified radius
     */
    fun observeReportsInRadius(
        latitude: Double,
        longitude: Double,
        radiusInMiles: Double
    ): Flow<List<FloodReport>>
}
