package edu.utap.ui.viewmodel

import android.annotation.SuppressLint
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.models.FloodReport
import edu.utap.models.DiscussionThread
import edu.utap.models.DiscussionMessage
import edu.utap.repository.DiscussionRepositoryInterface
import edu.utap.repository.FloodReportRepositoryInterface
import edu.utap.repository.FirestoreDiscussionRepository
import edu.utap.utils.LocationUtils
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.onSuccess

/**
 * ViewModel responsible for managing flood report state and operations.
 *
 * This ViewModel handles:
 * - Creating new flood reports
 * - Managing location data
 * - Handling photo uploads
 * - Managing report status
 * - User interactions with reports
 */
class FloodReportViewModel(
    internal val floodReportRepository: FloodReportRepositoryInterface,
    internal val authViewModel: AuthViewModelInterface,
    internal val locationUtils: LocationUtils,
    private val storageUtil: edu.utap.utils.FirebaseStorageUtilInterface,
    private val discussionRepository: DiscussionRepositoryInterface = FirestoreDiscussionRepository(FirebaseFirestore.getInstance())
) : ViewModel() {

    private val _activeFloodReports = MutableStateFlow<List<FloodReport>>(emptyList())
    val activeFloodReports: StateFlow<List<FloodReport>> get() = _activeFloodReports

    // State flows
    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _selectedPhotos = MutableStateFlow<List<String>>(emptyList())
    val selectedPhotos: StateFlow<List<String>> = _selectedPhotos

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber

    private val _severity = MutableStateFlow("medium")
    val severity: StateFlow<String> = _severity

    private val _waterDepth = MutableStateFlow(0.0)
    val waterDepth: StateFlow<Double> = _waterDepth

    private val _isRoadClosed = MutableStateFlow<Boolean?>(null)
    val isRoadClosed: StateFlow<Boolean?> = _isRoadClosed

    private val _canAccessOffice = MutableStateFlow<Boolean?>(null)
    val canAccessOffice: StateFlow<Boolean?> = _canAccessOffice

    private val _isManualLocation = MutableStateFlow(false)
    val isManualLocation: StateFlow<Boolean> = _isManualLocation

    private val _latitude = MutableStateFlow(0.0)
    val latitude: StateFlow<Double> = _latitude

    private val _longitude = MutableStateFlow(0.0)
    val longitude: StateFlow<Double> = _longitude

    // State for flood reports list
    private val _allReports = MutableStateFlow<List<FloodReport>>(emptyList())
    val allReports: StateFlow<List<FloodReport>> = _allReports

    private val _reportsLoading = MutableStateFlow(false)
    val reportsLoading: StateFlow<Boolean> = _reportsLoading

    private val _reportsError = MutableStateFlow<String?>(null)
    val reportsError: StateFlow<String?> = _reportsError

    private val _localFloodReports = MutableStateFlow<List<FloodReport>>(emptyList())
    val localFloodReports: StateFlow<List<FloodReport>> = _localFloodReports

    init {
        // Prefill email (or use anonymous fallback)
        val userEmail = authViewModel.getCurrentUser()?.email ?: "anon@ymous.com"
        _email.value = userEmail

        // Prefill location into latitude/longitude
        viewModelScope.launch {
            try {
                val loc = locationUtils.getCurrentLocation()
                _currentLocation.value = loc
                loc?.let {
                    _latitude.value = it.latitude
                    _longitude.value = it.longitude
                }
            } catch (_: Exception) {
                // Ignore location errors
            }
        }
        // Fetch all flood reports on initialization
        this.fetchAllReports()
    }

    /**
     * Updates the description of the flood report.
     *
     * @param newDescription The new description text
     */
    fun updateDescription(newDescription: String) {
        _description.value = newDescription
    }

    /**
     * Updates the email address for the flood report.
     *
     * @param newEmail The new email address
     */
    fun updateEmail(newEmail: String) {
        _email.value = newEmail
    }

    /**
     * Updates the phone number for the flood report.
     *
     * @param newPhoneNumber The new phone number
     */
    fun updatePhoneNumber(newPhoneNumber: String) {
        _phoneNumber.value = newPhoneNumber
    }

    /**
     * Updates whether the flood severity is high.
     *
     * @param isHigh Whether the flood severity is high
     */
    fun updateSeverity(newSeverity: String) {
        _severity.value = newSeverity
    }

    fun updateWaterDepth(newDepth: Double) {
        _waterDepth.value = newDepth
    }

    fun updateRoadClosed(isClosed: Boolean) {
        _isRoadClosed.value = isClosed
    }

    /**
     * Updates whether the user can access the office.
     *
     * @param canAccess Whether the user can access the office
     */
    fun updateOfficeAccess(canAccess: Boolean) {
        _canAccessOffice.value = canAccess
    }

    /**
     * Adds a photo to the selected photos list.
     *
     * @param photoUrl The URL of the photo to add
     */
    fun addPhoto(photoUrl: String) {
        _selectedPhotos.value = _selectedPhotos.value + photoUrl
    }

    /**
     * Removes a photo from the selected photos list.
     *
     * @param photoUrl The URL of the photo to remove
     */
    fun removePhoto(photoUrl: String) {
        _selectedPhotos.value = _selectedPhotos.value - photoUrl
    }

    /**
     * Toggles manual location mode.
     *
     * @param enabled Whether manual location mode should be enabled
     */
    fun setManualLocation(enabled: Boolean) {
        _isManualLocation.value = enabled
    }

    /**
     * Updates the latitude coordinate for manual location.
     *
     * @param lat The latitude coordinate
     */
    fun updateLatitude(lat: Double) {
        _latitude.value = lat
    }

    /**
     * Updates the longitude coordinate for manual location.
     *
     * @param lon The longitude coordinate
     */
    fun updateLongitude(lon: Double) {
        _longitude.value = lon
    }

    /**
     * Fetches all flood reports from the repository.
     */
    fun fetchAllReports() {
        viewModelScope.launch {
            _reportsLoading.value = true
            _reportsError.value = null

            try {
                floodReportRepository.observeAllReports()
                    .collect { reports ->
                        _allReports.value = reports
                        _activeFloodReports.value = reports
                        _reportsLoading.value = false
                    }
            } catch (e: Exception) {
                _reportsError.value = e.message ?: "Failed to load flood reports"
                _reportsLoading.value = false
            }
        }
    }

    fun fetchReportsInRadius(latitude: Double, longitude: Double, radiusKm: Double) {
        viewModelScope.launch {
            _reportsLoading.value = true
            _reportsError.value = null

            try {
                val firestoreReportsDeferred = floodReportRepository.getReportsInRadius(
                    latitude,
                    longitude,
                    radiusKm
                ).first()
                // _combinedFloodReports.value = firestoreReportsDeferred
                _reportsLoading.value = false
            } catch (e: Exception) {
                _reportsError.value = e.message ?: "Failed to load flood reports"
                _reportsLoading.value = false
            }
        }
    }

    fun refreshActiveFloodReports() {
        viewModelScope.launch {
            try {
                val reports = floodReportRepository.observeAllReports().first()
                val activeReports = reports.filter { it.status == "active" }
                _activeFloodReports.value = activeReports
            } catch (e: Exception) {
                _activeFloodReports.value = emptyList()
            }
        }
    }

    /**
     * Updates both latitude and longitude coordinates for manual location.
     *
     * @param lat The latitude coordinate
     * @param lon The longitude coordinate
     */
    fun updateManualLocation(lat: Double, lon: Double) {
        _latitude.value = lat
        _longitude.value = lon
    }

    /**
     * Submits a new flood report.
     *
     * This method creates a new flood report with the current state data
     * and saves it to both the local database and Firestore.
     */
    @RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION])
    fun submitReport() {
        viewModelScope.launch  {

            val sanAntonio = Location("manual").apply {
                latitude = 29.4241
                longitude = -98.4936
            }

            _reportState.value = ReportState.Loading
            try {
                val currentUser = authViewModel.getCurrentUser()
                // Determine location: manual override or background await
                val location = if (_isManualLocation.value) {
                    Location("manual").apply {
                        latitude = _latitude.value
                        longitude = _longitude.value
                    }
                } else {
                    // Perform blocking await on IO dispatcher to avoid main-thread restriction
                    withContext(Dispatchers.IO) {
                        Tasks.await(locationUtils.getLastKnownLocation())
                    }
                }

                val report = FloodReport(
                    reportId = UUID.randomUUID().toString(),
                    userId = currentUser?.userId ?: "anonymous",
                    latitude = location?.latitude ?: sanAntonio.latitude,
                    longitude = location?.longitude ?: sanAntonio.longitude,
                    description = _description.value,
                    photoUrls = _selectedPhotos.value,
                    status = "pending",
                    isManualLocation = _isManualLocation.value,
                    severity = _severity.value,
                    waterDepthInches = _waterDepth.value,
                    isRoadClosed = _isRoadClosed.value ?: false
                )

                val result = floodReportRepository.createReport(report)
                result.fold(
                    onSuccess = {
                        // --- Create a discussion thread for this flood report ---
                        val thread = DiscussionThread(
                            threadId = report.reportId, // 1:1 mapping
                            reportId = report.reportId,
                            createdBy = currentUser?.email ?: "anony@mous.com",
                            title = "Flood Report: ${report.description.take(40)}",
                            description = buildString {
                                append("Severity: ${report.severity}\n")
                                append("Water Depth: ${report.waterDepthInches} inches\n")
                                append("Road Closed: ${if (report.isRoadClosed) "Yes" else "No"}\n")
                                append("Details: ${report.description}")
                            },
                            category = "Flood Report"
                        )
                        discussionRepository.createThread(thread)
                        // --- End discussion thread creation ---
                        _reportState.value = ReportState.Success
                        // Reset form
                        _description.value = ""
                        _selectedPhotos.value = emptyList()
                        _isManualLocation.value = false
                    },
                    onFailure = { error ->
                        _reportState.value = ReportState.Error(error.message ?: "Failed to submit report")
                        // Do not rethrow, swallow failure
                    }
                )
            } catch (e: Exception) {
                _reportState.value = ReportState.Error(e.message ?: "An unexpected error occurred")
                // Swallow exception to prevent coroutine crash
            }
        }
    }

    /**
     * Confirms a flood report.
     *
     * @param reportId The ID of the report to confirm
     */
    fun confirmReport(reportId: String) {
        viewModelScope.launch {
            try {
                val reportResult = floodReportRepository.getReportById(reportId)
                reportResult.fold(
                    onSuccess = { report ->
                        val updatedReport = report.copy(
                            confirmedCount = report.confirmedCount + 1
                        )
                        floodReportRepository.updateReport(updatedReport)
                    },
                    onFailure = { error ->
                        _reportState.value =
                            ReportState.Error(error.message ?: "Failed to confirm report")
                    }
                )
            } catch (e: Exception) {
                _reportState.value = ReportState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    /**
     * Denies a flood report.
     *
     * @param reportId The ID of the report to deny
     */
    fun denyReport(reportId: String) {
        viewModelScope.launch {
            try {
                val reportResult = floodReportRepository.getReportById(reportId)
                reportResult.fold(
                    onSuccess = { report ->
                        val updatedReport = report.copy(
                            deniedCount = report.deniedCount + 1
                        )
                        floodReportRepository.updateReport(updatedReport)
                    },
                    onFailure = { error ->
                        _reportState.value =
                            ReportState.Error(error.message ?: "Failed to deny report")
                    }
                )
            } catch (e: Exception) {
                _reportState.value = ReportState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    /**
     * Uploads a photo for a flood report to Firebase Storage and updates the selected photos list.
     * @param context Android context
     * @param imageUri Uri of the image to upload
     * @param reportId The report ID to use in the storage path
     * @param onResult Callback with the download URL or error message
     */
    fun uploadFloodReportPhoto(
        context: android.content.Context,
        imageUri: android.net.Uri,
        reportId: String,
        onResult: (Result<String>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = storageUtil.uploadFloodReportImage(context, imageUri, reportId)
            if(result.isSuccess) {
                _selectedPhotos.value = _selectedPhotos.value + result.getOrThrow()
            }
            onResult(result)
        }
    }

    /**
     * Updates the status of a flood report (pending, active, inactive).
     * @param reportId The ID of the report to update
     * @param newStatus The new status to apply
     */
    fun setReportStatus(reportId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                val result = floodReportRepository.getReportById(reportId)
                result.fold(
                    onSuccess = { report ->
                        val updated = report.copy(status = newStatus)
                        floodReportRepository.updateReport(updated)
                    },
                    onFailure = { error ->
                        _reportState.value = ReportState.Error(error.message ?: "Failed to update status")
                    }
                )
            } catch (e: Exception) {
                _reportState.value = ReportState.Error(e.message ?: "Unexpected error")
            }
        }
    }
}

/**
 * Sealed class representing the possible states of a flood report operation.
 */
sealed class ReportState {
    object Idle : ReportState()
    object Loading : ReportState()
    object Success : ReportState()
    data class Error(val message: String) : ReportState()
}
