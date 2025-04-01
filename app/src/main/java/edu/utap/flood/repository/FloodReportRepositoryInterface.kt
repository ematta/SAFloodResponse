package edu.utap.flood.repository

import com.google.type.LatLng
import edu.utap.flood.model.FloodReport
import kotlinx.coroutines.flow.Flow

interface FloodReportRepositoryInterface {
    suspend fun createReport(report: FloodReport): Result<FloodReport>
    suspend fun getReportById(id: String): Result<FloodReport>
    fun observeAllReports(): Flow<List<FloodReport>>
    suspend fun updateReport(report: FloodReport): Result<FloodReport>
    suspend fun deleteReport(id: String): Result<Unit>
    fun getReportsInRadius(latitude: Double, longitude: Double, radius: Double)
}
