package edu.utap.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.utap.auth.model.AuthViewModelInterface
import edu.utap.flood.model.FloodReport
import edu.utap.flood.repository.FloodReportRepositoryInterface
import edu.utap.utils.LocationUtils
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    private val floodReportRepository: FloodReportRepositoryInterface,
    private val authViewModel: AuthViewModelInterface,
    private val locationUtils: LocationUtils
) : ViewModel() {

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
    
    private val _isFloodSeverityHigh = MutableStateFlow<Boolean?>(null)
    val isFloodSeverityHigh: StateFlow<Boolean?> = _isFloodSeverityHigh
    
    private val _canAccessOffice = MutableStateFlow<Boolean?>(null)
    val canAccessOffice: StateFlow<Boolean?> = _canAccessOffice

    private val _isManualLocation = MutableStateFlow(false)
    val isManualLocation: StateFlow<Boolean> = _isManualLocation

    private val _manualLatitude = MutableStateFlow(0.0)
    val manualLatitude: StateFlow<Double> = _manualLatitude

    private val _manualLongitude = MutableStateFlow(0.0)
    val manualLongitude: StateFlow<Double> = _manualLongitude

    init {
        // Request location updates
        locationUtils.requestLocationUpdates { location ->
            _currentLocation.value = location
        }
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
    fun updateFloodSeverity(isHigh: Boolean) {
        _isFloodSeverityHigh.value = isHigh
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
     * Updates the manual location coordinates.
     *
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     */
    fun updateManualLocation(latitude: Double, longitude: Double) {
        _manualLatitude.value = latitude
        _manualLongitude.value = longitude
    }

    /**
     * Submits a new flood report.
     *
     * This method creates a new flood report with the current state data
     * and saves it to both the local database and Firestore.
     */
    fun submitReport() {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading

            try {
                val currentUser = authViewModel.getCurrentUser()
                if (currentUser == null) {
                    _reportState.value = ReportState.Error("User not logged in")
                    return@launch
                }

                val location = if (_isManualLocation.value) {
                    Location("manual").apply {
                        latitude = _manualLatitude.value
                        longitude = _manualLongitude.value
                    }
                } else {
                    _currentLocation.value ?: throw Exception("Location not available")
                }

                val report = FloodReport(
                    reportId = UUID.randomUUID().toString(),
                    userId = currentUser.userId, // Corrected: Use userId field
                    latitude = location.latitude,
                    longitude = location.longitude,
                    description = _description.value,
                    photoUrls = _selectedPhotos.value,
                    status = "pending",
                    isManualLocation = _isManualLocation.value
                )

                val result = floodReportRepository.createReport(report)
                result.fold(
                    onSuccess = {
                        _reportState.value = ReportState.Success
                        // Reset form
                        _description.value = ""
                        _selectedPhotos.value = emptyList()
                        _isManualLocation.value = false
                    },
                    onFailure = { error ->
                        _reportState.value =
                            ReportState.Error(error.message ?: "Failed to submit report")
                    }
                )
            } catch (e: Exception) {
                _reportState.value = ReportState.Error(e.message ?: "An unexpected error occurred")
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
