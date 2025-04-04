package edu.utap.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import edu.utap.flood.model.FloodReport
import edu.utap.flood.repository.FloodReportRepositoryInterface
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FloodMapTestState(
    val floodReports: List<FloodReport> = emptyList(),
    val isSimulationMode: Boolean = false,
    val simulationSpeed: Float = 1.0f,
    val selectedReport: FloodReport? = null,
    val showReportDialog: Boolean = false,
    val currentLocation: LatLng? = null,
    val showGeometryPoints: Boolean = true,
    val terrainType: TerrainType = TerrainType.FLAT,
    val showMergedAreas: Boolean = true
)

enum class TerrainType {
    FLAT,
    HILLY,
    URBAN,
    RIVER;

    fun toSpreadMultiplier(): Float = when (this) {
        FLAT -> 1.0f
        HILLY -> 0.7f
        URBAN -> 0.5f
        RIVER -> 1.5f
    }
}

class FloodMapTestViewModel @Inject constructor(
    private val floodReportRepository: FloodReportRepositoryInterface
) : ViewModel() {
    private val _state = MutableStateFlow(FloodMapTestState())
    val state: StateFlow<FloodMapTestState> = _state.asStateFlow()

    private var reportsJob: Job? = null

    init {
        observeFloodReports()
    }

    private fun observeFloodReports() {
        reportsJob?.cancel()
        reportsJob = viewModelScope.launch {
            floodReportRepository.observeAllReports()
                .catch { e ->
                    Log.e("FloodMapVM", "Error observing reports", e)
                }
                .collect { reports ->
                    _state.update { it.copy(floodReports = reports) }
                }
        }
    }

    fun selectReport(report: FloodReport?) {
        _state.update { it.copy(selectedReport = report) }
    }

    fun toggleReportDialog() {
        _state.update { it.copy(showReportDialog = !it.showReportDialog) }
    }

    fun setCurrentLocation(location: LatLng) {
        _state.update { it.copy(currentLocation = location) }
    }

    override fun onCleared() {
        super.onCleared()
        reportsJob?.cancel()
    }
}
