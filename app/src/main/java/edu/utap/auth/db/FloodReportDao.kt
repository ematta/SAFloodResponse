package edu.utap.auth.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FloodReportDao {
    @Query("SELECT * FROM flood_reports")
    fun getAllReports(): Flow<List<FloodReportEntity>>
    
    @Query("SELECT * FROM flood_reports WHERE reportId = :reportId")
    suspend fun getReportById(reportId: String): FloodReportEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: FloodReportEntity)
} 