package edu.utap.repository

import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.flood.model.FloodReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * Implementation of [FloodReportRepositoryInterface] that handles Firestore operations for flood reports.
 *
 * @property firestore The Firestore database instance.
 */
import android.util.Log
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

private const val TAG = "FloodReportRepository"

class FloodReportRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : FloodReportRepositoryInterface {

    private val reportsCollection = firestore.collection("flood_reports").also {
        Log.d(TAG, "Using Firestore collection: flood_reports")
    }

    override suspend fun createReport(report: FloodReport): Result<FloodReport> = try {
        reportsCollection.document(report.reportId)
            .set(report)
            .await()
        Result.success(report)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getReportById(reportId: String): Result<FloodReport> = try {
        val document = reportsCollection.document(reportId).get().await()
        if (document.exists()) {
            Result.success(document.toObject<FloodReport>()!!)
        } else {
            Result.failure(Exception("Report not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateReport(report: FloodReport): Result<FloodReport> = try {
        reportsCollection.document(report.reportId)
            .set(report)
            .await()
        Result.success(report)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteReport(reportId: String): Result<Unit> = try {
        reportsCollection.document(reportId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getReportsInRadius(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): Flow<List<FloodReport>> = callbackFlow {
        val radiusInDegrees = radius / 111.0

        Log.d(TAG, "Querying reports within $radius km of ($latitude, $longitude)")
        Log.d(TAG, "Radius in degrees: $radiusInDegrees")
        Log.d(TAG, "Latitude range: [${latitude - radiusInDegrees}, ${latitude + radiusInDegrees}]")
        Log.d(TAG, "Longitude range: [${longitude - radiusInDegrees}, ${longitude + radiusInDegrees}]")

        val listener = reportsCollection
            .whereGreaterThanOrEqualTo("latitude", latitude - radiusInDegrees)
            .whereLessThanOrEqualTo("latitude", latitude + radiusInDegrees)
            .whereGreaterThanOrEqualTo("longitude", longitude - radiusInDegrees)
            .whereLessThanOrEqualTo("longitude", longitude + radiusInDegrees)
            .also { query ->
                Log.d(TAG, "Firestore query: ${query.toString()}")
            }
            .addSnapshotListener { querySnapshot, error ->
                Log.d(TAG, "Firestore query executed - error: ${error?.message}, docs: ${querySnapshot?.documents?.size}")
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val reports = querySnapshot?.documents?.mapNotNull { document ->
                    document.toObject<FloodReport>()
                } ?: emptyList()

                trySend(reports)
            }

        awaitClose { listener.remove() }
    }

    override fun observeAllReports(): Flow<List<FloodReport>> = callbackFlow {
        val listener = reportsCollection
            .orderBy("created_at")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val reports = querySnapshot?.documents?.mapNotNull { document ->
                    try {
                        val report = document.toObject<FloodReport>()
                        Log.d(TAG, "Fetched report: ${report?.reportId} at (${report?.latitude}, ${report?.longitude})")
                        report
                    } catch (e: Exception) {
                        Log.d(TAG, "Error parsing document ${document.id}: ${e.message}")
                        null
                    }
                } ?: emptyList<FloodReport>().also {
                    Log.d(TAG, "No reports found in query snapshot")
                }

                trySend(reports)
            }

        awaitClose { listener.remove() }
    }
}
