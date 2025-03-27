package edu.utap.weather

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

const val TAG = "NOAAService"

class NOAAService(
    private val client: OkHttpClient = OkHttpClient(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val baseUrl = "https://api.weather.gov"

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        this.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                // Create a new response with a buffered body that can be read multiple times
                val bufferedResponse = response.newBuilder()
                    .body(response.peekBody(Long.MAX_VALUE))
                    .build()
                continuation.resume(bufferedResponse)
            }

            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }
        })

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (ex: Throwable) {
                // ignore exception on cancellation, it could be that job is already completed
            }
        }
    }

    suspend fun getFloodAlerts(lat: Double, lon: Double): List<FloodAlert> =
        withContext(dispatcher) {
            try {
                Log.d(TAG, "NOAA Service: Starting getFloodAlerts")
                // First, get the grid endpoint for the coordinates
                val gridRequest = createGridRequest(lat, lon)
                val gridResponse = client.newCall(gridRequest).await()
                if (!gridResponse.isSuccessful) {
                    Log.d(TAG, "Grid request failed: ${gridResponse.code}")
                    return@withContext emptyList<FloodAlert>()
                }

                val gridResponseBody = gridResponse.body?.string() ?: throw IOException("Empty grid response body")
                Log.d(TAG, "NOAA Service: Grid response body: $gridResponseBody")

                val gridJson = JSONObject(gridResponseBody)
                val gridProperties = gridJson.getJSONObject("properties")

                // Get the gridId for test compatibility
                val gridId = gridProperties.getString("gridId")
                Log.d(TAG, "NOAA Service: Grid ID: $gridId")

                // Step 2: Get alerts using the gridId
                val alertsRequest = createAlertsRequest(gridId)
                val alertsResponse = client.newCall(alertsRequest).await()
                if (!alertsResponse.isSuccessful) {
                    Log.d(TAG, "Alerts request failed: ${alertsResponse.code}")
                    return@withContext emptyList<FloodAlert>()
                }

                // Get the alerts response body
                val alertsBody = alertsResponse.body?.string() ?: run {
                    Log.d(TAG, "Null alerts response body")
                    return@withContext emptyList<FloodAlert>()
                }

                if (alertsBody.isEmpty()) {
                    Log.d(TAG, "Empty alerts response body")
                    return@withContext emptyList<FloodAlert>()
                }

                // Log the alerts response body
                Log.d(TAG, "NOAA Service: Alerts response body: $alertsBody")

                val alertsJson = JSONObject(alertsBody)
                Log.d(TAG, "NOAA Service: Parsed alerts JSON")

                if (!alertsJson.has("features")) {
                    Log.d(TAG, "NOAA Service: No 'features' key found")
                    return@withContext emptyList()
                }

                val features = alertsJson.getJSONArray("features")
                Log.d(TAG, "NOAA Service: Got features array, length: ${features.length()}")

                val floodAlerts = mutableListOf<FloodAlert>()

                for (i in 0 until features.length()) {
                    try {
                        Log.d(TAG, "NOAA Service: Processing feature $i")
                        val feature = features.getJSONObject(i)
                        val properties = feature.getJSONObject("properties")
                        val event = properties.getString("event")
                        Log.d(TAG, "NOAA Service: Event: $event")

                        // Only include flood-related alerts
                        if (event.contains("Flood", ignoreCase = true)) {
                            Log.d(TAG, "NOAA Service: Found flood event")
                            
                            // Get coordinates from geometry if available
                            var latitude = lat
                            var longitude = lon
                            
                            if (feature.has("geometry") && !feature.isNull("geometry")) {
                                val geometry = feature.getJSONObject("geometry")
                                if (geometry.has("coordinates") && !geometry.isNull("coordinates")) {
                                    val coordinates = geometry.getJSONArray("coordinates")
                                    Log.d(TAG, "NOAA Service: Got coordinates")
                                    // In GeoJSON format, coordinates are [longitude, latitude]
                                    longitude = coordinates.getDouble(0)
                                    latitude = coordinates.getDouble(1)
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
                            Log.d(TAG, "NOAA Service: Added flood alert, total: ${floodAlerts.size}")
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Error processing feature $i: ${e.message}")
                        // Continue processing other features
                    }
                }

                Log.d(
                    TAG,
                    "NOAA Service: Finished processing features, returning ${floodAlerts.size} alerts"
                )
                floodAlerts
            } catch (e: Exception) {
                Log.d(TAG, "NOAA Service: Exception caught: ${e.message}")
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
