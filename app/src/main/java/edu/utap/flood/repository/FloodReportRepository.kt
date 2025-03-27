package edu.utap.flood.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.flood.db.FloodReportDao
import edu.utap.flood.db.FloodReportEntity
import edu.utap.flood.model.FloodReport
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Implementation of FloodReportRepositoryInterface that handles both Firestore
 * and local database operations for flood reports.
 */
class FloodReportRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val floodReportDao: FloodReportDao
) : FloodReportRepositoryInterface {

    private val reportsCollection = firestore.collection("flood_reports")

    override suspend fun createReport(report: FloodReport): Result<FloodReport> = try {
        // Create report in Firestore
        reportsCollection.document(report.reportId)
            .set(report)
            .await()

        // Create report in local database
        val entity = report.toEntity()
        floodReportDao.insertReport(entity)

        Result.success(report)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getReportById(reportId: String): Result<FloodReport> {
        return try {
            // Try to get from local database first
            val localReport = floodReportDao.getReportById(reportId)
            if (localReport != null) {
                return Result.success(localReport.toModel())
            }

            // If not found locally, get from Firestore
            val document = reportsCollection.document(reportId).get().await()
            if (!document.exists()) {
                return Result.failure(Exception("Report not found"))
            }

            val report = document.toObject(FloodReport::class.java)
                ?: throw Exception("Failed to parse report data")

            // Cache in local database
            floodReportDao.insertReport(report.toEntity())

            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReportsInRadius(
        latitude: Double,
        longitude: Double,
        radiusInMiles: Double
    ): Result<List<FloodReport>> = try {
        // Get reports from local database
        val localReports = floodReportDao.getReportsInRadius(latitude, longitude, radiusInMiles)
        Result.success(localReports.map { it.toModel() })
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateReport(report: FloodReport): Result<FloodReport> = try {
        // Update in Firestore
        reportsCollection.document(report.reportId)
            .set(report)
            .await()

        // Update in local database
        floodReportDao.updateReport(report.toEntity())

        Result.success(report)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteReport(reportId: String): Result<Unit> = try {
        // Delete from Firestore
        reportsCollection.document(reportId)
            .delete()
            .await()

        // Delete from local database
        floodReportDao.getReportById(reportId)?.let { entity ->
            floodReportDao.deleteReport(entity)
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeAllReports(): Flow<List<FloodReport>> =
        floodReportDao.getAllReports().map { entities ->
            entities.map { it.toModel() }
        }

    override fun observeReportsInRadius(
        latitude: Double,
        longitude: Double,
        radiusInMiles: Double
    ): Flow<List<FloodReport>> = floodReportDao.getAllReports().map { entities ->
        entities.filter { entity ->
            calculateDistance(
                latitude,
                longitude,
                entity.latitude,
                entity.longitude
            ) <= radiusInMiles
        }.map { it.toModel() }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 3959.0 // Earth's radius in miles
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)

        val a = kotlin.math.sin(deltaLat / 2) * kotlin.math.sin(deltaLat / 2) +
            kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
            kotlin.math.sin(deltaLon / 2) * kotlin.math.sin(deltaLon / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    private fun FloodReport.toEntity(): FloodReportEntity = FloodReportEntity(
        reportId = reportId,
        userId = userId,
        latitude = latitude,
        longitude = longitude,
        description = description,
        photoUrls = photoUrls,
        status = status,
        createdAt = Date(createdAt.seconds * 1000),
        updatedAt = Date(updatedAt.seconds * 1000),
        isManualLocation = isManualLocation,
        confirmedCount = confirmedCount,
        deniedCount = deniedCount
    )

    private fun FloodReportEntity.toModel(): FloodReport = FloodReport(
        reportId = reportId,
        userId = userId,
        latitude = latitude,
        longitude = longitude,
        description = description,
        photoUrls = photoUrls,
        status = status,
        createdAt = Timestamp(createdAt),
        updatedAt = Timestamp(updatedAt),
        isManualLocation = isManualLocation,
        confirmedCount = confirmedCount,
        deniedCount = deniedCount
    )
}
