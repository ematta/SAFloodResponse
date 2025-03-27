package edu.utap.weather

import android.util.Log
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val TEST_TAG = "NOAAServiceTest"

@OptIn(ExperimentalCoroutinesApi::class)
class NOAAServiceTest {
    private lateinit var noaaService: NOAAService
    private lateinit var mockClient: OkHttpClient
    private lateinit var mockCall: Call
    private val jsonMediaType = "application/json".toMediaType()
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        mockClient = mockk(relaxed = true)
        mockCall = mockk(relaxed = true)
        every { mockClient.newCall(any()) } returns mockCall
        noaaService = NOAAService(mockClient, testDispatcher)
    }

    private fun createResponse(
        isSuccessful: Boolean,
        code: Int,
        body: String
    ): Response {
        Log.d(TEST_TAG, "Creating response with body: $body")
        val request = Request.Builder()
            .url("https://api.weather.gov")
            .build()
        val responseBody = body.toResponseBody(jsonMediaType)
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(if (isSuccessful) 200 else code)
            .message(if (isSuccessful) "OK" else "Error")
            .body(responseBody)
            .build()
    }

    @Test
    fun `getFloodAlerts returns empty list when grid request fails`() = runTest {
        // Given
        val response = createResponse(
            isSuccessful = false,
            code = 500,
            body = "{}"
        )
        coEvery { mockCall.execute() } returns response

        // When
        val result = noaaService.getFloodAlerts(29.4241, -98.4936)
        advanceUntilIdle()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFloodAlerts returns empty list when alerts request fails`() = runTest {
        // Given
        val gridJson = JSONObject().apply {
            put("properties", JSONObject().apply {
                put("gridId", "EWX")
                put("gridX", 123)
                put("gridY", 456)
            })
        }
        
        var firstCall = true
        coEvery { mockCall.execute() } answers {
            if (firstCall) {
                firstCall = false
                Log.d(TEST_TAG, "Returning grid response: ${gridJson.toString()}")
                createResponse(
                    isSuccessful = true,
                    code = 200,
                    body = gridJson.toString()
                )
            } else {
                Log.d(TEST_TAG, "Returning failed alerts response")
                createResponse(
                    isSuccessful = false,
                    code = 500,
                    body = "{}"
                )
            }
        }

        // When
        val result = noaaService.getFloodAlerts(29.4241, -98.4936)
        advanceUntilIdle()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFloodAlerts returns flood alerts when successful`() = runTest {
        // Given
        val gridJson = JSONObject().apply {
            put("properties", JSONObject().apply {
                put("gridId", "EWX")
                put("gridX", 123)
                put("gridY", 456)
            })
        }

        val alertsJson = JSONObject().apply {
            put("features", JSONArray().apply {
                put(JSONObject().apply {
                    put("properties", JSONObject().apply {
                        put("id", "test-id")
                        put("event", "Flood Warning")
                        put("headline", "Test Flood Warning")
                        put("description", "Test Description")
                        put("severity", "Severe")
                        put("areaDesc", "Test Area")
                        put("sent", 1234567890L)
                    })
                    put("geometry", JSONObject().apply {
                        put("type", "Point")
                        put("coordinates", JSONArray().apply {
                            put(-98.4936)
                            put(29.4241)
                        })
                    })
                })
            })
        }
        
        var firstCall = true
        coEvery { mockCall.execute() } answers {
            val request = firstArg<Request>()
            Log.d(TEST_TAG, "Received request for URL: ${request.url}")
            
            if (firstCall) {
                firstCall = false
                Log.d(TEST_TAG, "Returning grid response: ${gridJson.toString()}")
                createResponse(
                    isSuccessful = true,
                    code = 200,
                    body = gridJson.toString()
                )
            } else {
                Log.d(TEST_TAG, "Returning alerts response: ${alertsJson.toString()}")
                createResponse(
                    isSuccessful = true,
                    code = 200,
                    body = alertsJson.toString()
                )
            }
        }

        // When
        val result = noaaService.getFloodAlerts(29.4241, -98.4936)
        advanceUntilIdle()

        // Then
        Log.d(TEST_TAG, "Result size: ${result.size}")
        assertEquals(1, result.size)
        val alert = result[0]
        assertEquals("test-id", alert.id)
        assertEquals("Test Flood Warning", alert.title)
        assertEquals("Test Description", alert.description)
        assertEquals("Severe", alert.severity)
        assertEquals("Test Area", alert.location)
        assertEquals(29.4241, alert.latitude, "Latitude should match the test data")
        assertEquals(-98.4936, alert.longitude, "Longitude should match the test data")
        assertEquals(1234567890L, alert.timestamp)
    }

    @Test
    fun `getFloodAlerts filters out non-flood alerts`() = runTest {
        // Given
        val gridJson = JSONObject().apply {
            put("properties", JSONObject().apply {
                put("gridId", "EWX")
                put("gridX", 123)
                put("gridY", 456)
            })
        }

        val alertsJson = JSONObject().apply {
            put("features", JSONArray().apply {
                // Flood alert
                put(JSONObject().apply {
                    put("properties", JSONObject().apply {
                        put("id", "flood-id")
                        put("event", "Flood Warning")
                        put("headline", "Test Flood Warning")
                        put("description", "Test Description")
                        put("severity", "Severe")
                        put("areaDesc", "Test Area")
                        put("sent", 1234567890L)
                    })
                    put("geometry", JSONObject().apply {
                        put("type", "Point")
                        put("coordinates", JSONArray().apply {
                            put(-98.4936)
                            put(29.4241)
                        })
                    })
                })
                // Non-flood alert
                put(JSONObject().apply {
                    put("properties", JSONObject().apply {
                        put("id", "storm-id")
                        put("event", "Severe Thunderstorm Warning")
                        put("headline", "Test Storm Warning")
                        put("description", "Test Description")
                        put("severity", "Severe")
                        put("areaDesc", "Test Area")
                        put("sent", 1234567890L)
                    })
                    put("geometry", JSONObject().apply {
                        put("type", "Point")
                        put("coordinates", JSONArray().apply {
                            put(-98.4936)
                            put(29.4241)
                        })
                    })
                })
            })
        }
        
        var firstCall = true
        coEvery { mockCall.execute() } answers {
            val request = firstArg<Request>()
            Log.d(TEST_TAG, "Received request for URL: ${request.url}")
            
            if (firstCall) {
                firstCall = false
                Log.d(TEST_TAG, "Returning grid response: ${gridJson.toString()}")
                createResponse(
                    isSuccessful = true,
                    code = 200,
                    body = gridJson.toString()
                )
            } else {
                Log.d(TEST_TAG, "Returning alerts response: ${alertsJson.toString()}")
                createResponse(
                    isSuccessful = true,
                    code = 200,
                    body = alertsJson.toString()
                )
            }
        }

        // When
        val result = noaaService.getFloodAlerts(29.4241, -98.4936)
        advanceUntilIdle()

        // Then
        Log.d(TEST_TAG, "Result size: ${result.size}")
        assertEquals(1, result.size)
        val alert = result[0]
        assertEquals("flood-id", alert.id)
        assertEquals("Test Flood Warning", alert.title)
    }
}
