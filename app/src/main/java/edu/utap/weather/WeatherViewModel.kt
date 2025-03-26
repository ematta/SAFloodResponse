package edu.utap.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {
    private val noaaService = NOAAService()
    
    private val _floodAlerts = MutableStateFlow<List<FloodAlert>>(emptyList())
    val floodAlerts: StateFlow<List<FloodAlert>> = _floodAlerts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun fetchFloodAlerts(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _floodAlerts.value = noaaService.getFloodAlerts(lat, lon)
            } catch (e: Exception) {
                _error.value = "Failed to fetch flood alerts: ${e.message}"
                _floodAlerts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 