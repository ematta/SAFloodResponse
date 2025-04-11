package edu.utap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing weather and flood alert data.
 *
 * Fetches flood alerts from NOAA and exposes loading and error states.
 *
 * @param noaaService The NOAA service used to fetch flood alerts.
 */
class WeatherViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchFloodAlerts(lat: Double, lon: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to fetch flood alerts: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
