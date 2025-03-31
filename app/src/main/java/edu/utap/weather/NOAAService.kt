package edu.utap.weather

import android.util.Log
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneOffset
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
            .header("Accept", "application/geo+json")
            .build()

        // Log request details
        val requestHeaders = request.headers.joinToString("\n\t") { "${it.first}: ${it.second}" }
        Log.d("NOAAService_makeRequest",
            """
            |Making request to $url
            |Thread: ${Thread.currentThread().name}
            |Headers:
            |\t$requestHeaders
            """.trimMargin())

        try {
            val response = client.newCall(request).execute()
            
            // Log response details
            val responseHeaders = response.headers.joinToString("\n\t") { "${it.first}: ${it.second}" }
            Log.d("NOAAService_makeRequest",
                """
                |Received response for $url
                |Status: ${response.code} ${response.message}
                |Headers:
                |\t$responseHeaders
                """.trimMargin())

            val body = response.body?.string() ?: throw IOException("Empty response body")
            Log.v("NOAAService_makeRequest", "Response body: $body")

            if (!response.isSuccessful) {
                val error = IOException("Request failed with code ${response.code}")
                Log.e("NOAAService_makeRequest",
                    "Request failed: ${error.message}\n${Log.getStackTraceString(error)}")
                throw error
            }

            JSONObject(body)
        } catch (e: Exception) {
            Log.e("NOAAService_makeRequest",
                "Error making request to $url: ${e.message}\n${Log.getStackTraceString(e)}")
            throw e
        }
    }

    suspend fun getFloodAlerts(latitude: Double, longitude: Double): List<FloodAlert> {
        try {
            Log.d(TAG, "Getting flood alerts for lat: $latitude, lon: $longitude")

            // Get alerts for the point
            val alertsUrl = "$BASE_URL/alerts?point=$latitude,$longitude"
            Log.d(TAG, "Fetching alerts from URL: $alertsUrl")
            val alertsResponse = makeRequest(alertsUrl)
            Log.d(TAG, "Full alerts response: $alertsResponse")
            val features = alertsResponse.getJSONArray("features")
            Log.d(TAG, "Got ${features.length()} total alerts from response")

            val alerts = mutableListOf<FloodAlert>()
            for (i in 0 until features.length()) {
                try {
                    val feature = features.getJSONObject(i)
                    Log.d(TAG, "Processing feature: $feature")
                    val alertProperties = feature.getJSONObject("properties")
                    Log.d(TAG, "Alert properties: $alertProperties")
                    val event = alertProperties.getString("event")
                    Log.d(TAG, "Processing alert $i - event: $event")

                    // Only process flood-related alerts
                    if (!event.contains("Flood", ignoreCase = true)) {
                        Log.d(TAG, "Skipping non-flood alert: $event")
                        continue
                    }

                    Log.d(TAG, "Found flood alert: $event")

                    // Default to the input coordinates if geometry is not available
                    var alertLatitude = latitude
                    var alertLongitude = longitude

                    try {
                        val geometry = feature.getJSONObject("geometry")
                        Log.d(TAG, "Geometry: $geometry")
                        if (geometry.getString("type").contains("Point", ignoreCase = true)) {
                            val coordinates = geometry.getJSONArray("coordinates")
                            Log.d(TAG, "Coordinates: $coordinates")
                            // GeoJSON format is [longitude, latitude]
                            alertLongitude = coordinates.getDouble(0)
                            alertLatitude = coordinates.getDouble(1)
                            Log.d(
                                TAG,
                                "Extracted coordinates: lat=$alertLatitude, lon=$alertLongitude"
                            )
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Could not parse geometry, using input coordinates", e)
                    }

                    val alert = FloodAlert(
                        id = alertProperties.getString("id"),
                        title = alertProperties.getString("headline"),
                        description = alertProperties.getString("description"),
                        severity = alertProperties.getString("severity"),
                        location = alertProperties.getString("areaDesc"),
                        latitude = alertLatitude,
                        longitude = alertLongitude,
                        timestamp = parseTimestamp(alertProperties.getString("sent"))
                    )
                    Log.d(TAG, "Created flood alert: $alert")
                    alerts.add(alert)
                    Log.d(TAG, "Added flood alert to list, current count: ${alerts.size}")
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

    private fun parseTimestamp(timestamp: String): Long {
        val zonedDateTime = ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME)
        return zonedDateTime.toInstant().toEpochMilli()
    }
}
