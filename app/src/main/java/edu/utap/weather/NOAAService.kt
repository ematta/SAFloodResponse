package edu.utap.weather

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

class NOAAService(
    private val client: OkHttpClient = OkHttpClient(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val baseUrl = "https://api.weather.gov"

    suspend fun getFloodAlerts(lat: Double, lon: Double): List<FloodAlert> =
        withContext(dispatcher) {
            try {
                println("NOAA Service: Starting getFloodAlerts") // Log start
                // First, get the grid endpoint for the coordinates
                val gridRequest = createGridRequest(lat, lon)
                val gridResponse = client.newCall(gridRequest).execute()
                if (!gridResponse.isSuccessful) {
                    System.err.println("Grid request failed: ${gridResponse.code}")
                    throw IOException("Failed to get grid endpoint: ${gridResponse.code}")
                }

                val gridResponseBody =
                    gridResponse.body?.string() ?: throw IOException("Empty grid response body")
                println("NOAA Service: Grid response body: $gridResponseBody") // Log grid response

                val gridJson = JSONObject(gridResponseBody)
                val gridProperties = gridJson.getJSONObject("properties")
                // val gridId = gridProperties.getString("gridId") // We need zoneId, not gridId for alerts
                // println("NOAA Service: Grid ID: $gridId") // Log grid ID

                // Step 2: Get the county URL to find the zone ID
                val countyUrl = gridProperties.getString("county")
                println("NOAA Service: County URL: $countyUrl") // Log county URL
                val countyRequest = Request.Builder().url(countyUrl).header("User-Agent", "SAFloodResponse/1.0").build()
                val countyResponse = client.newCall(countyRequest).execute()
                if (!countyResponse.isSuccessful) {
                    System.err.println("County request failed: ${countyResponse.code}")
                    throw IOException("Failed to get county details: ${countyResponse.code}")
                }
                val countyResponseBody = countyResponse.body?.string() ?: throw IOException("Empty county response body")
                println("NOAA Service: County response body: $countyResponseBody") // Log county response
                val countyJson = JSONObject(countyResponseBody)
                val countyProperties = countyJson.getJSONObject("properties")
                // Assuming 'id' in county properties is the zone ID. Check NOAA API if this is correct.
                // Common keys might be 'id', 'zoneIdentifier', 'cwa' + 'zone' number etc. Let's assume 'id' for now.
                val zoneId = countyProperties.getString("id")
                println("NOAA Service: Zone ID: $zoneId") // Log zone ID

                // Step 3: Now get the alerts for this zone
                val alertsRequest = createAlertsRequest(zoneId) // Use zoneId
                val alertsResponse = client.newCall(alertsRequest).execute()
                if (!alertsResponse.isSuccessful) {
                    System.err.println("Alerts request failed: ${alertsResponse.code}")
                    throw IOException("Failed to get alerts: ${alertsResponse.code}")
                }

                val alertsResponseBody =
                    alertsResponse.body?.string() ?: throw IOException("Empty alerts response body")
                println("NOAA Service: Alerts response body: $alertsResponseBody") // Log alerts response

                val alertsJson = JSONObject(alertsResponseBody)
                println("NOAA Service: Parsed alerts JSON") // Log after parsing alerts JSON

                if (!alertsJson.has("features")) {
                    println("NOAA Service: No 'features' key found") // Log missing key
                    return@withContext emptyList()
                }

                val features = alertsJson.getJSONArray("features")
                println("NOAA Service: Got features array, length: ${features.length()}") // Log features length

                val floodAlerts = mutableListOf<FloodAlert>()

                for (i in 0 until features.length()) {
                    println("NOAA Service: Processing feature $i") // Log loop iteration
                    val feature = features.getJSONObject(i)
                    val properties = feature.getJSONObject("properties")
                    val event = properties.getString("event")
                    println("NOAA Service: Event: $event") // Log event name

                    // Only include flood-related alerts
                    if (event.contains("Flood", ignoreCase = true)) {
                        println("NOAA Service: Found flood event") // Log flood event found
                        val geometry = feature.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        println("NOAA Service: Got coordinates") // Log coordinates obtained

                        // Create a flood alert with the correct coordinate mapping
                        // In GeoJSON format, coordinates are [longitude, latitude]
                        val longitude = coordinates.getDouble(0) // Extract longitude from JSON
                        val latitude = coordinates.getDouble(1)  // Extract latitude from JSON

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
                        println("NOAA Service: Added flood alert") // Log alert added
                    }
                }

                println("NOAA Service: Finished processing features, returning ${floodAlerts.size} alerts") // Log final count
                floodAlerts
            } catch (e: Exception) {
                println("NOAA Service: Exception caught: ${e.message}") // Log exception
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
