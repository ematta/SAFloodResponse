package edu.utap.flood.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import edu.utap.flood.model.FloodReport
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "FirestoreFloodReportRepo"

class FirestoreFloodReportRepository @Inject constructor(
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

    override suspend fun getReportById(id: String): Result<FloodReport> = try {
        val document = reportsCollection.document(id).get().await()
        if (document.exists()) {
            Result.success(document.toObject<FloodReport>()!!)
        } else {
            Result.failure(Exception("Report not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
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
                    } catch (e: Exception) {
                        Log.d(TAG, "Error parsing document ${document.id}: ${e.message}")
                        null
                    }
                } ?: emptyList<FloodReport>().also {
                    Log.d(TAG, "No reports found in query snapshot")
                }
                
                trySend(reports as List<FloodReport>)
            }
        
        awaitClose { listener.remove() }
    }

    override suspend fun updateReport(report: FloodReport): Result<FloodReport> = try {
        reportsCollection.document(report.reportId)
            .set(report)
            .await()
        Result.success(report)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteReport(id: String): Result<Unit> = try {
        reportsCollection.document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getReportsInRadius(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): Flow<List<FloodReport>> = callbackFlow {
        // Convert radius from miles to degrees (approximate)
        val radiusInDegrees = radius / 69.0
        
        Log.d(TAG, "Querying reports within $radius miles of ($latitude, $longitude)")
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
}
