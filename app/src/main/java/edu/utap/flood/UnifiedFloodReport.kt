package edu.utap.flood

import com.google.android.gms.maps.model.LatLng

/**
 * Data class representing a unified flood report.
 *
 * Combines internal user reports and external NOAA/NWS alerts into a common format.
 *
 * @property id Unique identifier for the report.
 * @property latitude Latitude coordinate.
 * @property longitude Longitude coordinate.
 * @property timestamp Time of the report/event in milliseconds since epoch.
 * @property severity Severity level as a string.
 * @property description Description of the report.
 * @property sources List of sources contributing to this report (e.g., "Internal", "NWS").
 */
data class UnifiedFloodReport(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val severity: String,
    val description: String,
    val sources: List<String>
) {
    /**
     * Converts the report's coordinates to a [LatLng] object.
     *
     * @return The [LatLng] representation of the report's location.
     */
    fun toLatLng(): com.google.android.gms.maps.model.LatLng =
        com.google.android.gms.maps.model.LatLng(latitude, longitude)
}
