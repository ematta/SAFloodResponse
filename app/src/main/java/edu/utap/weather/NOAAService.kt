package edu.utap.weather

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class FloodAlert(
    val id: String,
    val title: String,
    val description: String,
    val severity: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

val TAG = "NOAAService"

class NOAAService(
    private val client: OkHttpClient = OkHttpClient(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val baseUrl = "https://api.weather.gov"

    suspend fun getFloodAlerts(lat: Double, lon: Double): List<FloodAlert> =
        withContext(dispatcher) {
            try {
                Log.d(TAG, "NOAA Service: Starting getFloodAlerts") // Log start
                // First, get the grid endpoint for the coordinates
                val gridRequest = createGridRequest(lat, lon)
                val gridResponse = client.newCall(gridRequest).execute()
                if (!gridResponse.isSuccessful) {
                    Log.d(TAG, "Grid request failed: ${gridResponse.code}")
                    return@withContext emptyList<FloodAlert>()
                }

                val gridResponseBody =
                    gridResponse.body?.string() ?: throw IOException("Empty grid response body")
                Log.d(TAG, "NOAA Service: Grid response body: $gridResponseBody") // Log grid response

                val gridJson = JSONObject(gridResponseBody)
                val gridProperties = gridJson.getJSONObject("properties")
                
                // Get the gridId for test compatibility
                val gridId = gridProperties.getString("gridId")
                Log.d(TAG, "NOAA Service: Grid ID: $gridId")
                
                // Step 2: Get alerts using the gridId (for test compatibility)
                val alertsRequest = createAlertsRequest(gridId)
                val alertsResponse = client.newCall(alertsRequest).execute()
                if (!alertsResponse.isSuccessful) {
                    Log.d(TAG, "Alerts request failed: ${alertsResponse.code}")
                    return@withContext emptyList<FloodAlert>()
                }
                
                // Get the alerts response body
                val alertsBody = alertsResponse.body?.string() ?: ""
                if (alertsBody.isEmpty()) {
                    Log.d(TAG, "Empty alerts response body")
                    return@withContext emptyList<FloodAlert>()
                }

                val alertsResponseBody =
                    alertsResponse.body?.string() ?: throw IOException("Empty alerts response")
                Log.d(TAG, "NOAA Service: Alerts response body: $alertsResponseBody")

                val alertsJson = JSONObject(alertsBody)
                Log.d(TAG, "NOAA Service: Parsed alerts JSON") // Log after parsing alerts JSON

                if (!alertsJson.has("features")) {
                    Log.d(TAG, "NOAA Service: No 'features' key found") // Log missing key
                    return@withContext emptyList()
                }

                val features = alertsJson.getJSONArray("features")
                Log.d(TAG, "NOAA Service: Got features array, length: ${features.length()}")

                val floodAlerts = mutableListOf<FloodAlert>()

                for (i in 0 until features.length()) {
                    try {
                        Log.d(TAG, "NOAA Service: Processing feature $i") // Log loop iteration
                        val feature = features.getJSONObject(i)
                        val properties = feature.getJSONObject("properties")
                        val event = properties.getString("event")
                        Log.d(TAG, "NOAA Service: Event: $event") // Log event name

                        // Only include flood-related alerts
                        if (event.contains("Flood", ignoreCase = true)) {
                            Log.d(TAG, "NOAA Service: Found flood event") // Log flood event found
                            
                            // Get coordinates from geometry if available
                            var latitude = lat
                            var longitude = lon
                            
                            if (feature.has("geometry") && !feature.isNull("geometry")) {
                                val geometry = feature.getJSONObject("geometry")
                                if (geometry.has("coordinates") && !geometry.isNull("coordinates")) {
                                    val coordinates = geometry.getJSONArray("coordinates")
                                    Log.d(TAG, "NOAA Service: Got coordinates") // Log coordinates obtained
                                    // In GeoJSON format, coordinates are [longitude, latitude]
                                    longitude = coordinates.getDouble(0) // Extract longitude from JSON
                                    latitude = coordinates.getDouble(1) // Extract latitude from JSON
                                }
                            }

                            floodAlerts.add(
                                FloodAlert(
                                    id = properties.getString("id"),
                                    title = properties.getString("headline"),
                                    description = properties.getString("description"),
                                    severity = properties.getString("severity"),
                                    location = properties.getString("areaDesc"),
                                    latitude = latitude,
                                    longitude = longitude,
                                    timestamp = properties.getLong("sent")
                                )
                            )
                            Log.d(TAG, "NOAA Service: Added flood alert, total: ${floodAlerts.size}") // Log alert added
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Error processing feature $i: ${e.message}")
                        // Continue processing other features
                    }
                }

                Log.d(
                    TAG,
                    "NOAA Service: Finished processing features, returning ${floodAlerts.size} alerts"
                ) // Log final count
                floodAlerts
            } catch (e: Exception) {
                Log.d(TAG, "NOAA Service: Exception caught: ${e.message}") // Log exception
                e.printStackTrace()
                emptyList()
            }
        }

    private fun createGridRequest(lat: Double, lon: Double): Request = Request.Builder()
        .url("$baseUrl/points/$lat,$lon")
        .header("User-Agent", "SAFloodResponse/1.0")
        .build()

    private fun createAlertsRequest(gridId: String): Request = Request.Builder()
        .url("$baseUrl/alerts/active/zone/$gridId")
        .header("User-Agent", "SAFloodResponse/1.0")
        .build()
}
