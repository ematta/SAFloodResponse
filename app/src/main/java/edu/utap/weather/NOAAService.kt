package edu.utap.weather

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

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

class NOAAService(
    private val client: OkHttpClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val TAG = "NOAAService"
        private const val BASE_URL = "https://api.weather.gov"
    }

    private suspend fun makeRequest(url: String): JSONObject = withContext(dispatcher) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SAFloodResponse/1.0")
            .build()

        try {
            Log.d(TAG, "Making request to $url")
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw IOException("Empty response body")
            Log.d(TAG, "Response body: $body")
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Request failed with code ${response.code}")
                throw IOException("Request failed with code ${response.code}")
            }

            JSONObject(body)
        } catch (e: Exception) {
            Log.e(TAG, "Error making request to $url", e)
            throw e
        }
    }

    suspend fun getFloodAlerts(latitude: Double, longitude: Double): List<FloodAlert> {
        try {
            Log.d(TAG, "Getting flood alerts for lat: $latitude, lon: $longitude")
            
            // First, get the grid coordinates
            val pointsUrl = "$BASE_URL/points/$latitude,$longitude"
            val gridResponse = makeRequest(pointsUrl)
            val properties = gridResponse.getJSONObject("properties")
            val gridId = properties.getString("gridId")
            val gridX = properties.getInt("gridX")
            val gridY = properties.getInt("gridY")
            
            Log.d(TAG, "Got grid info - gridId: $gridId, gridX: $gridX, gridY: $gridY")

            // Then, get the alerts for that grid
            val alertsUrl = "$BASE_URL/alerts/active/zone/$gridId"
            val alertsResponse = makeRequest(alertsUrl)
            val features = alertsResponse.getJSONArray("features")
            Log.d(TAG, "Got ${features.length()} total alerts")
            
            val alerts = mutableListOf<FloodAlert>()
            for (i in 0 until features.length()) {
                try {
                    val feature = features.getJSONObject(i)
                    val alertProperties = feature.getJSONObject("properties")
                    val event = alertProperties.getString("event")
                    Log.d(TAG, "Processing alert $i - event: $event")
                    
                    // Only process flood-related alerts
                    if (!event.contains("Flood", ignoreCase = true)) {
                        Log.d(TAG, "Skipping non-flood alert: $event")
                        continue
                    }

                    Log.d(TAG, "Found flood alert: $event")
                    val geometry = feature.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")
                    
                    // Extract coordinates from the geometry
                    // GeoJSON format is [longitude, latitude]
                    val alertLongitude = coordinates.getDouble(0)
                    val alertLatitude = coordinates.getDouble(1)
                    
                    alerts.add(
                        FloodAlert(
                            id = alertProperties.getString("id"),
                            title = alertProperties.getString("headline"),
                            description = alertProperties.getString("description"),
                            severity = alertProperties.getString("severity"),
                            location = alertProperties.getString("areaDesc"),
                            latitude = alertLatitude,
                            longitude = alertLongitude,
                            timestamp = alertProperties.getLong("sent")
                        )
                    )
                    Log.d(TAG, "Added flood alert, current count: ${alerts.size}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing feature $i", e)
                    e.printStackTrace()
                }
            }
            
            Log.d(TAG, "Returning ${alerts.size} flood alerts")
            return alerts
        } catch (e: Exception) {
            Log.e(TAG, "Error getting flood alerts", e)
            return emptyList()
        }
    }
}
