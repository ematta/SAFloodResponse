package edu.utap.models

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.UUID

/**
 * Comprehensive data class representing a flood report,
 * combining Firestore persistence with geospatial simulation data.
 *
 * @property reportId Unique identifier for the flood report.
 * @property userId ID of the user who submitted the report.
 * @property latitude Latitude coordinate of the flood location.
 * @property longitude Longitude coordinate of the flood location.
 * @property description Description of the flood event.
 * @property photoUrls List of URLs of associated photos.
 * @property status Current status of the flood report ("pending", "confirmed", "denied").
 * @property createdAt Timestamp when the report was created.
 * @property updatedAt Timestamp when the report was last updated.
 * @property isManualLocation Indicates if the location was manually entered.
 * @property confirmedCount Number of users who confirmed the report.
 * @property deniedCount Number of users who denied the report.
 * @property severity Severity of the flood ("low", "medium", "high", "extreme").
 * @property waterDepthInches Depth of water in inches.
 * @property isRoadClosed Indicates if the road is closed.
 * @property geometryPoints List of LatLng points for geospatial simulation.
 * @property radius Radius of the geospatial simulation.
 * @property maxRadius Maximum radius of the geospatial simulation.
 * @property spreadRate Spread rate of the geospatial simulation.
 * @property isExpanding Indicates if the geospatial simulation is expanding.
 * @property mergedReports List of report IDs that were merged into this report.
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
    // Persisted severity string ("low", "medium", "high", "extreme").
    val severity: String = "medium",
    @get:PropertyName("water_depth_inches")
    @set:PropertyName("water_depth_inches")
    var waterDepthInches: Double = 0.0,
    @get:PropertyName("is_road_closed")
    @set:PropertyName("is_road_closed")
    var isRoadClosed: Boolean = false,
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
