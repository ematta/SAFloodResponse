package edu.utap.flood.repository

import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.flood.model.FloodReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * Implementation of FloodReportRepositoryInterface that handles Firestore operations for flood reports.
 */
class FloodReportRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : FloodReportRepositoryInterface {

    private val reportsCollection = firestore.collection("flood_reports")

    override suspend fun createReport(report: FloodReport): Result<FloodReport> = try {
        // Create report in Firestore
        reportsCollection.document(report.reportId)
            .set(report)
            .await()

        Result.success(report)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getReportById(reportId: String): Result<FloodReport> {
        return try {
            // Get from Firestore
            val document = reportsCollection.document(reportId).get().await()
            if (!document.exists()) {
                return Result.failure(Exception("Report not found"))
            }

            val report = document.toObject(FloodReport::class.java)
                ?: throw Exception("Failed to parse report data")

            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReport(report: FloodReport): Result<FloodReport> = try {
        // Update in Firestore
        reportsCollection.document(report.reportId)
            .set(report)
            .await()

        Result.success(report)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteReport(reportId: String): Result<Unit> = try {
        // Delete from Firestore
        reportsCollection.document(reportId)
            .delete()
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getReportsInRadius(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): Flow<List<FloodReport>> {
        // TODO: Implement Firestore query for reports in radius
        return flowOf(emptyList())
    }

    override fun observeAllReports(): Flow<List<FloodReport>> {
        // TODO: Implement Firestore real-time updates
        return flowOf(emptyList())
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

}
