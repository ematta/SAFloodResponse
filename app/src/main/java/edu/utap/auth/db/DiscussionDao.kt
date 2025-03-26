package edu.utap.auth.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for discussion thread operations.
 *
 * This interface defines the database operations that can be performed on discussion threads.
 * It provides methods to query and insert discussion data related to flood reports.
 * The methods are implemented by Room at compile time.
 */
@Dao
interface DiscussionDao {
    /**
     * Gets all discussion threads for a specific flood report as a Flow.
     *
     * Using Flow allows the UI to observe changes to the data and update automatically
     * when the underlying database changes.
     *
     * @param reportId The ID of the flood report to get discussions for
     * @return A Flow emitting the list of discussion threads for the specified report
     */
    @Query("SELECT * FROM discussions WHERE reportId = :reportId")
    fun getDiscussionsForReport(reportId: String): Flow<List<DiscussionEntity>>

    /**
     * Inserts or updates a discussion thread in the database.
     *
     * If a discussion with the same ID already exists, it will be replaced.
     *
     * @param discussion The discussion thread to insert or update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscussion(discussion: DiscussionEntity)
}
