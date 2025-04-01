package edu.utap.flood.repository

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

class FirestoreFloodReportRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : FloodReportRepositoryInterface {
    private val reportsCollection = firestore.collection("flood_reports")

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
                    document.toObject<FloodReport>()
                } ?: emptyList()
                
                trySend(reports)
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
    ) {
        // TODO: Implement this function to fetch reports within a specified radius
    }
}
