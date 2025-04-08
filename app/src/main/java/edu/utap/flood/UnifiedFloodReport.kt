package edu.utap.flood

import com.google.android.gms.maps.model.LatLng

data class UnifiedFloodReport(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val severity: String,
    val description: String,
    val sources: List<String>
) {
    fun toLatLng(): com.google.android.gms.maps.model.LatLng =
        com.google.android.gms.maps.model.LatLng(latitude, longitude)
}
