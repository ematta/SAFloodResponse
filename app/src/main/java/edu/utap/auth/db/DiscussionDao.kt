package edu.utap.auth.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscussionDao {
    @Query("SELECT * FROM discussions WHERE reportId = :reportId")
    fun getDiscussionsForReport(reportId: String): Flow<List<DiscussionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscussion(discussion: DiscussionEntity)
} 