package edu.utap.flood

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import java.util.UUID
import kotlin.math.max

/**
 * Data class representing a user-reported or system-generated flood event.
 *
 * Includes location, severity, description, timestamp, radius, water level,
 * spread rate, and merged report IDs.
 */
data class FloodReport(
    val id: String = UUID.randomUUID().toString(),
    val location: LatLng,
    val severity: FloodSeverity,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val radius: Float = 1000f,
    val waterLevel: Float = 0.5f,
    val geometryPoints: List<LatLng> = emptyList(),
    val isExpanding: Boolean = true,
    val maxRadius: Float = 5000f,
    val spreadRate: Float = 1.0f,
    val mergedReports: List<String> = emptyList()
) {
    /**
     * Merges this flood report with another, combining their data.
     *
     * - Calculates a weighted average location based on water levels.
     * - Chooses the higher severity.
     * - Combines descriptions.
     * - Takes the larger radius.
     * - Averages water levels.
     * - Takes the faster spread rate.
     * - Combines merged report IDs.
     *
     * @param other The other [FloodReport] to merge with.
     * @return A new merged [FloodReport].
     */
    fun mergeWith(other: FloodReport): FloodReport {
        // Calculate new center as weighted average based on water levels
        val totalWaterLevel = waterLevel + other.waterLevel
        val newLat =
            (location.latitude * waterLevel + other.location.latitude * other.waterLevel) /
                totalWaterLevel
        val newLng =
            (location.longitude * waterLevel + other.location.longitude * other.waterLevel) /
                totalWaterLevel
        val newLocation = LatLng(newLat, newLng)

        // Take the higher severity
        val newSeverity = if (severity.ordinal >
            other.severity.ordinal
        ) {
            severity
        } else {
            other.severity
        }

        // Combine descriptions
        val newDescription = "$description + ${other.description}"

        // Take the larger radius
        val newRadius = max(radius, other.radius)

        // Calculate new water level (average)
        val newWaterLevel = (waterLevel + other.waterLevel) / 2f

        // Take the faster spread rate
        val newSpreadRate = max(spreadRate, other.spreadRate)

        // Combine merged reports
        val newMergedReports = mergedReports + other.mergedReports + other.id

        return copy(
            location = newLocation,
            severity = newSeverity,
            description = newDescription,
            radius = newRadius,
            waterLevel = newWaterLevel,
            spreadRate = newSpreadRate,
            mergedReports = newMergedReports
        )
    }

    /**
     * Checks if this flood report overlaps with another based on their radii.
     *
     * @param other The other [FloodReport] to check overlap with.
     * @return `true` if the reports overlap, `false` otherwise.
     */
    fun overlapsWith(other: FloodReport): Boolean {
        // Calculate distance between centers
        val distance = calculateDistance(location, other.location)
        // Check if the sum of radii is greater than the distance
        return distance <= (radius + other.radius)
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            point1.latitude,
            point1.longitude,
            point2.latitude,
            point2.longitude,
            results
        )
        return results[0]
    }
}

/**
 * Enum representing severity levels of a flood event.
 */
enum class FloodSeverity {
    LOW,
    MEDIUM,
    HIGH,
    EXTREME;

    /**
     * Converts severity to a representative ARGB color (80% opacity).
     */
    fun toColor(): Long = when (this) {
        LOW -> 0xCC00FF00 // Green with 80% opacity
        MEDIUM -> 0xCC00FFFF // Cyan with 80% opacity
        HIGH -> 0xCCFFA500 // Orange with 80% opacity
        EXTREME -> 0xCCFF0000 // Red with 80% opacity
    }

    /**
     * Converts severity to a normalized water level (0.0 to 1.0).
     */
    fun toWaterLevel(): Float = when (this) {
        LOW -> 0.25f
        MEDIUM -> 0.5f
        HIGH -> 0.75f
        EXTREME -> 1.0f
    }

    /**
     * Converts severity to a spread rate multiplier.
     */
    fun toSpreadRate(): Float = when (this) {
        LOW -> 0.5f
        MEDIUM -> 1.0f
        HIGH -> 1.5f
        EXTREME -> 2.0f
    }
}
