package edu.utap.models

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.UUID

/**
 * Comprehensive data class representing a flood report,
 * combining Firestore persistence with geospatial simulation data.
 */
data class FloodReport(
    @DocumentId
    val reportId: String = UUID.randomUUID().toString(),

    val userId: String = "",

    val latitude: Double = 0.0,
    val longitude: Double = 0.0,

    val description: String = "",

    val photoUrls: List<String> = emptyList(),

    val status: String = "pending",

    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Timestamp = Timestamp.now(),

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Timestamp = Timestamp.now(),

    @get:PropertyName("is_manual_location")
    @set:PropertyName("is_manual_location")
    var isManualLocation: Boolean = false,

    @get:PropertyName("confirmed_count")
    @set:PropertyName("confirmed_count")
    var confirmedCount: Int = 0,

    @get:PropertyName("denied_count")
    @set:PropertyName("denied_count")
    var deniedCount: Int = 0,

    /**
     * Persisted severity string ("low", "medium", "high", "extreme").
     */
    val severity: String = "medium",

    @get:PropertyName("water_depth_inches")
    @set:PropertyName("water_depth_inches")
    var waterDepthInches: Double = 0.0,

    @get:PropertyName("is_road_closed")
    @set:PropertyName("is_road_closed")
    var isRoadClosed: Boolean = false,

    // --- Additional geospatial simulation fields ---

    val geometryPoints: List<LatLng> = emptyList(),

    val radius: Float = 1000f,

    val maxRadius: Float = 5000f,

    val spreadRate: Float = 1.0f,

    val isExpanding: Boolean = true,

    val mergedReports: List<String> = emptyList()
) {

    /**
     * Helper to get LatLng object from latitude and longitude.
     */
    val location: LatLng
        get() = LatLng(latitude, longitude)

}
