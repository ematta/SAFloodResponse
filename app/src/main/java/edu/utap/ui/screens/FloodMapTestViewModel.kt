package edu.utap.ui.screens

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.min
import kotlin.random.Random

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
    FLAT, HILLY, URBAN, RIVER;

    fun toSpreadMultiplier(): Float {
        return when (this) {
            FLAT -> 1.0f
            HILLY -> 0.7f
            URBAN -> 0.5f
            RIVER -> 1.5f
        }
    }
}

class FloodMapTestViewModel : ViewModel() {
    private val _state = MutableStateFlow(FloodMapTestState())
    val state: StateFlow<FloodMapTestState> = _state.asStateFlow()

    fun toggleSimulationMode() {
        _state.update { it.copy(isSimulationMode = !it.isSimulationMode) }
    }

    fun setSimulationSpeed(speed: Float) {
        _state.update { it.copy(simulationSpeed = speed.coerceIn(0.1f, 5f)) }
    }

    fun addFloodReport(report: FloodReport) {
        val updatedReport = report.copy(
            waterLevel = report.severity.toWaterLevel(),
            spreadRate = report.severity.toSpreadRate() * _state.value.terrainType.toSpreadMultiplier(),
            geometryPoints = generateGeometryPoints(report.location, report.radius)
        )
        
        // Check for overlaps with existing reports
        val reports = _state.value.floodReports.toMutableList()
        var mergedReport = updatedReport
        
        for (i in reports.indices) {
            if (mergedReport.overlapsWith(reports[i])) {
                mergedReport = mergedReport.mergeWith(reports[i])
            }
        }

        _state.update { it.copy(floodReports = reports + mergedReport) }
    }

    fun removeFloodReport(reportId: String) {
        _state.update { it.copy(floodReports = it.floodReports.filter { it.id != reportId }) }
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

    fun toggleGeometryPoints() {
        _state.update { it.copy(showGeometryPoints = !it.showGeometryPoints) }
    }

    fun toggleMergedAreas() {
        _state.update { it.copy(showMergedAreas = !it.showMergedAreas) }
    }

    fun setTerrainType(type: TerrainType) {
        _state.update { 
            it.copy(
                terrainType = type,
                floodReports = it.floodReports.map { report ->
                    report.copy(
                        spreadRate = report.severity.toSpreadRate() * type.toSpreadMultiplier()
                    )
                }
            )
        }
    }

    // Simulation functions
    fun simulateFloodSpread() {
        if (!_state.value.isSimulationMode) return

        val reports = _state.value.floodReports.map { report ->
            if (!report.isExpanding) return@map report

            val newRadius = min(
                report.radius * (1 + report.spreadRate * _state.value.simulationSpeed * 0.05f),
                report.maxRadius
            )

            // Stop expanding if max radius is reached
            val isExpanding = newRadius < report.maxRadius

            report.copy(
                radius = newRadius,
                isExpanding = isExpanding,
                geometryPoints = generateGeometryPoints(report.location, newRadius)
            )
        }

        // Check for and handle merging
        val mergedReports = mutableListOf<FloodReport>()
        val reportsToProcess = reports.toMutableList()

        while (reportsToProcess.isNotEmpty()) {
            var currentReport = reportsToProcess.removeAt(0)
            var i = 0
            while (i < reportsToProcess.size) {
                if (currentReport.overlapsWith(reportsToProcess[i])) {
                    currentReport = currentReport.mergeWith(reportsToProcess[i])
                    reportsToProcess.removeAt(i)
                } else {
                    i++
                }
            }
            mergedReports.add(currentReport)
        }

        _state.update { it.copy(floodReports = mergedReports) }
    }

    fun clearSimulation() {
        _state.update { it.copy(floodReports = emptyList()) }
    }

    private fun generateGeometryPoints(center: LatLng, radius: Float): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val numPoints = 32 // Number of points to create a smooth circle
        val radiusInDegrees = radius / 111320f // Convert meters to degrees (approximate)

        for (i in 0 until numPoints) {
            val angle = (2 * Math.PI * i) / numPoints
            val lat = center.latitude + (radiusInDegrees * Math.sin(angle))
            val lng = center.longitude + (radiusInDegrees * Math.cos(angle) / Math.cos(Math.toRadians(center.latitude)))
            points.add(LatLng(lat, lng))
        }

        // Close the polygon
        points.add(points[0])
        return points
    }
} 
