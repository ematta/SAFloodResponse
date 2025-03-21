package edu.utap.repository
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.models.FloodReport
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "FloodReportRepository"

/**
 * Interface for interacting with flood report data.
 * This interface defines the operations that can be performed on flood reports,
 * such as creating, retrieving, updating, deleting, and observing reports.
 */
interface FloodReportRepositoryInterface {
    suspend fun createReport(report: FloodReport): Result<FloodReport>
    suspend fun getReportById(id: String): Result<FloodReport>
    fun observeAllReports(): Flow<List<FloodReport>>
    suspend fun updateReport(report: FloodReport): Result<FloodReport>
    suspend fun deleteReport(id: String): Result<Unit>
    fun getReportsInRadius(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Flow<List<FloodReport>>
}

/**
 * Repository class for managing flood reports.
 *
 * This class handles interactions with the Firestore database for creating, reading, updating,
 * deleting, and observing flood reports. It also provides functionality to retrieve reports within
 * a specified radius and to observe all reports in real-time.
 *
 * @property firestore The FirebaseFirestore instance used to interact with the database.
 */
class FloodReportRepository @Inject constructor(private val firestore: FirebaseFirestore) :
    BaseRepository(),
    FloodReportRepositoryInterface {

    private val reportsCollection = firestore.collection("flood_reports").also {
        Log.d(TAG, "Using Firestore collection: flood_reports")
    }

    /**
     * Creates a new flood report in Firestore.
     *
     * This function attempts to create a new document in the `reportsCollection` with the provided
     * `FloodReport` data. The document ID will be set to the `reportId` of the input `report`.
     *
     * @param report The `FloodReport` object to be stored in Firestore. The `reportId` field of this
     *               object will be used as the document ID in the collection.
     * @return A `Result` object that either contains the successfully stored `FloodReport` or an
     *         exception indicating the failure reason.
     *         - `Result.success(FloodReport)`: If the report was successfully created.
     *         - `Result.failure(Exception)`: If an error occurred during the operation, such as network
     *           issues or Firestore errors. The exception will detail the specific error.
     *
     * @throws Exception If the underlying Firestore operation fails.
     */
    override suspend fun createReport(report: FloodReport): Result<FloodReport> =
        safeFirestoreCall {
            reportsCollection.document(report.reportId)
                .set(report)
                .await()
            report
        }

    /**
     * Retrieves a FloodReport from Firestore by its ID.
     *
     * This function attempts to fetch a document from the 'reportsCollection' in Firestore with the specified ID.
     * If the document exists, it will attempt to convert it to a FloodReport domain object.
     * If the document does not exist or the conversion fails, an appropriate exception is thrown.
     *
     * @param id The ID of the FloodReport to retrieve.
     * @return A Result object containing either:
     *         - A successful Result with the FloodReport object if found and parsed successfully.
     *         - A failed Result with an Exception if:
     *           - The document does not exist. (Exception: "Report not found")
     *           - The document exists but cannot be parsed into a FloodReport. (Exception: "Failed to parse report")
     *           - Any other errors occur during the Firestore operation.
     *
     * @throws Exception if there is an error with Firestore or parsing the document.
     * @see safeFirestoreCall For details on how Firestore operations are handled safely.
     * @see FloodReport The domain object representing a flood report.
     * @see CollectionReference The Firebase Firestore collection where the documents are stored.
     */
    override suspend fun getReportById(id: String): Result<FloodReport> = safeFirestoreCall {
        val document = reportsCollection.document(id).get().await()
        if (document.exists()) {
            document.toDomainObject<FloodReport>() ?: throw Exception("Failed to parse report")
        } else {
            throw Exception("Report not found")
        }
    }

    /**
     * Updates an existing flood report in the Firestore database.
     *
     * This function takes a [FloodReport] object and attempts to update the corresponding
     * document in the "reports" collection of the Firestore database. It uses the
     * `reportId` property of the [FloodReport] object to identify the document to be updated.
     *
     * @param report The [FloodReport] object containing the updated information. The `reportId`
     *               must match an existing document in the "reports" collection.
     * @return A [Result] object that either:
     *          - Contains the updated [FloodReport] if the update was successful.
     *          - Contains an exception if the update failed. The exception type will vary depending on the
     *            nature of the failure (e.g., network error, document not found, permission denied).
     *
     * @throws Exception Any exception thrown during the Firestore operation will be caught and wrapped in the Result.
     *
     * Example Usage:
     * ```kotlin
     * val updatedReport = FloodReport("report123", "New Location", "High", ... )
     * val result = updateReport(updatedReport)
     *
     * result.onSuccess {
     *     println("Report updated successfully: $it")
     * }.onFailure {
     *     println("Failed to update report: ${it.message}")
     * }
     * ```
     */
    override suspend fun updateReport(report: FloodReport): Result<FloodReport> =
        safeFirestoreCall {
            reportsCollection.document(report.reportId)
                .set(report)
                .await()
            report
        }

    /**
     * Deletes a report from the Firestore database.
     *
     * @param id The ID of the report to delete.
     * @return A [Result] indicating the success or failure of the operation.
     *   - [Result.Success] with [Unit] if the report was successfully deleted.
     *   - [Result.Failure] if an error occurred during the deletion process.
     *   The failure result will contain a [Throwable] representing the underlying error.
     *
     * @throws Exception If any exception occurs during the Firestore operation. This exception will be wrapped inside the Result.Failure
     *
     * Usage Example:
     * ```kotlin
     * val result = repository.deleteReport("reportId123")
     * result.onSuccess {
     *     println("Report deleted successfully.")
     * }.onFailure { exception ->
     *     println("Error deleting report: ${exception.message}")
     * }
     * ```
     */
    override suspend fun deleteReport(id: String): Result<Unit> = safeFirestoreCall {
        reportsCollection.document(id).delete().await()
        Unit
    }

    /**
     * Retrieves a stream of flood reports within a specified radius of a given location.
     *
     * This function queries a Firestore collection for `FloodReport` objects that fall within
     * a circular area defined by the provided latitude, longitude, and radius. It uses a
     * snapshot listener to provide real-time updates to the list of reports as they are
     * added, modified, or removed within the specified area.
     *
     * The function calculates the latitude and longitude boundaries based on the radius in kilometers,
     * converts it into degrees, and then constructs a Firestore query to filter documents within those ranges.
     * Note that this is a simple bounding box approach. This is an approximation and for large radiuses, might not be perfectly accurate
     * because of the curvature of the earth. For higher accuracy, consider using geohashing.
     *
     * @param latitude The latitude of the center of the search area.
     * @param longitude The longitude of the center of the search area.
     * @param radius The radius (in kilometers) around the center point to search within.
     * @return A `Flow` emitting a `List` of `FloodReport` objects within the specified radius.
     *         The flow will emit a new list whenever the data in the Firestore collection changes
     *         within the query parameters.
     *         The flow will close with an error if an error occurs during the snapshot listening.
     *         The flow will be active until the calling coroutine scope is cancelled or the callbackFlow is closed.
     *
     * @throws Exception if there is any error during the listening to the firestore changes.
     */
    override fun getReportsInRadius(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): Flow<List<FloodReport>> = callbackFlow {
        val radiusInDegrees = radius / 111.0

        Log.d(TAG, "Querying reports within $radius km of ($latitude, $longitude)")
        Log.d(TAG, "Radius in degrees: $radiusInDegrees")
        Log.d(TAG, "Latitude range: [${latitude - radiusInDegrees}, ${latitude + radiusInDegrees}]")
        Log.d(
            TAG,
            "Longitude range: [${longitude - radiusInDegrees}, ${longitude + radiusInDegrees}]"
        )

        val listener = reportsCollection
            .whereGreaterThanOrEqualTo("latitude", latitude - radiusInDegrees)
            .whereLessThanOrEqualTo("latitude", latitude + radiusInDegrees)
            .whereGreaterThanOrEqualTo("longitude", longitude - radiusInDegrees)
            .whereLessThanOrEqualTo("longitude", longitude + radiusInDegrees)
            .also { query ->
                Log.d(TAG, "Firestore query: $query")
            }
            .addSnapshotListener { querySnapshot, error ->
                Log.d(
                    TAG,
                    "Firestore query executed - error: ${error?.message}, docs: ${querySnapshot?.documents?.size}"
                )
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val reports = querySnapshot?.documents?.mapNotNull { document ->
                    document.toDomainObject<FloodReport>()
                } ?: emptyList()

                trySend(reports)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observes all flood reports from the Firestore collection in real-time.
     *
     * This function establishes a snapshot listener on the Firestore "reports" collection,
     * ordered by the "created_at" field in ascending order. It then transforms the
     * received snapshot data into a list of `FloodReport` domain objects and emits
     * them through a Kotlin Flow.
     *
     * The function handles potential errors during the snapshot retrieval and
     * document parsing, logging them for debugging purposes. It also gracefully
     * closes the listener when the flow is cancelled.
     *
     * @return A [Flow] emitting lists of [FloodReport] objects. Each emission
     *         represents an update to the collection of flood reports in Firestore.
     *         The flow will emit an empty list if no reports are found. If an error
     *         occurs during the snapshot listening process, the flow will be closed
     *         with the respective error.
     *
     * @throws Exception if an error occurs during document parsing, which is then caught
     *         and handled within the function, logged and ignored for that specific document.
     *
     * Example Usage:
     *
     * ```kotlin
     * lifecycleScope.launch {
     *     reportRepository.observeAllReports().collect { reports ->
     *         // Handle the list of reports
     *         for (report in reports) {
     *             println("Report ID: ${report.reportId}")
     *         }
     *     }
     * }
     * ```
     */
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
                        val report = document.toDomainObject<FloodReport>()
                        Log.d(
                            TAG,
                            "Fetched report: ${report?.reportId} at (${report?.latitude}, ${report?.longitude})"
                        )
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
