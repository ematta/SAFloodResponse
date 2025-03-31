package edu.utap.db

import kotlinx.coroutines.flow.Flow

class FloodReportDaoAdapter(private val authFloodReportDao: FloodReportDao) : FloodReportDao {
    override fun getAllReports(): Flow<List<FloodReportEntity>> {
        return authFloodReportDao.getAllReports()
    }

    override suspend fun getReportById(reportId: String): FloodReportEntity? =
        authFloodReportDao.getReportById(reportId)

    override suspend fun getReportsInRadius(
        latitude: Double,
        longitude: Double,
        radiusInMiles: Double
    ): List<FloodReportEntity> = authFloodReportDao.getReportsInRadius(
        latitude = latitude,
        longitude = longitude,
        radiusInMiles = radiusInMiles
    )

    override suspend fun insertReport(report: FloodReportEntity) {
        authFloodReportDao.insertReport(report)
    }

    override suspend fun updateReport(report: FloodReportEntity) {
        authFloodReportDao.updateReport(report)
    }

    override suspend fun deleteReport(report: FloodReportEntity) {
        authFloodReportDao.deleteReport(report)
    }
}
