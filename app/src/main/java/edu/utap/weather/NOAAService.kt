package edu.utap.weather

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
    private val client: OkHttpClient = OkHttpClient(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val baseUrl = "https://api.weather.gov"
    
    suspend fun getFloodAlerts(lat: Double, lon: Double): List<FloodAlert> = withContext(dispatcher) {
        try {
            // First, get the grid endpoint for the coordinates
            val gridRequest = createGridRequest(lat, lon)
            val gridResponse = client.newCall(gridRequest).execute()
            if (!gridResponse.isSuccessful) {
                System.err.println("Grid request failed: ${gridResponse.code}")
                throw IOException("Failed to get grid endpoint: ${gridResponse.code}")
            }

            val gridResponseBody = gridResponse.body?.string() ?: throw IOException("Empty grid response body")
            System.out.println("Grid response: $gridResponseBody")
            val gridJson = JSONObject(gridResponseBody)
            val properties = gridJson.getJSONObject("properties")
            val gridId = properties.getString("gridId")
            System.out.println("Grid ID: $gridId")

            // Now get the alerts for this grid
            val alertsRequest = createAlertsRequest(gridId)
            val alertsResponse = client.newCall(alertsRequest).execute()
            if (!alertsResponse.isSuccessful) {
                System.err.println("Alerts request failed: ${alertsResponse.code}")
                throw IOException("Failed to get alerts: ${alertsResponse.code}")
            }

            val alertsResponseBody = alertsResponse.body?.string() ?: throw IOException("Empty alerts response body")
            System.out.println("Alerts response: $alertsResponseBody")
            val alertsJson = JSONObject(alertsResponseBody)
            
            if (!alertsJson.has("features")) {
                System.out.println("No features in alerts response")
                return@withContext emptyList()
            }
            
            val features = alertsJson.getJSONArray("features")
            System.out.println("Found ${features.length()} features")
            val floodAlerts = mutableListOf<FloodAlert>()

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val properties = feature.getJSONObject("properties")
                val event = properties.getString("event")
                System.out.println("Processing event: $event")
                
                // Only include flood-related alerts
                if (event.contains("Flood", ignoreCase = true)) {
                    System.out.println("Found flood alert: $event")
                    val geometry = feature.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")
                    
                    floodAlerts.add(FloodAlert(
                        id = properties.getString("id"),
                        title = properties.getString("headline"),
                        description = properties.getString("description"),
                        severity = properties.getString("severity"),
                        location = properties.getString("areaDesc"),
                        latitude = coordinates.getDouble(1),
                        longitude = coordinates.getDouble(0),
                        timestamp = properties.getLong("sent")
                    ))
                }
            }

            System.out.println("Returning ${floodAlerts.size} flood alerts")
            floodAlerts
        } catch (e: Exception) {
            System.err.println("Error in getFloodAlerts: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    private fun createGridRequest(lat: Double, lon: Double): Request {
        return Request.Builder()
            .url("$baseUrl/points/$lat,$lon")
            .header("User-Agent", "SAFloodResponse/1.0")
            .build()
    }

    private fun createAlertsRequest(gridId: String): Request {
        return Request.Builder()
            .url("$baseUrl/alerts/active/zone/$gridId")
            .header("User-Agent", "SAFloodResponse/1.0")
            .build()
    }
}