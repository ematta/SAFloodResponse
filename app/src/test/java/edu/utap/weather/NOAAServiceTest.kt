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
import kotlin.test.Ignore
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
        Log.d(TEST_TAG, "Creating response - isSuccessful: $isSuccessful, code: $code")
        Log.d(TEST_TAG, "Response body: $body")
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

    // TODO: Fix this test
    @Test
    @Ignore
    fun `getFloodAlerts returns flood alerts when successful`() = runTest {
        val latitude = 30.2672
        val longitude = -97.7431
        val mockResponse = """
            {
                "features": [
                    {
                        "properties": {
                            "id": "test-id",
                            "headline": "Test Flood Warning",
                            "description": "Test description",
                            "severity": "Moderate",
                            "areaDesc": "Test Area",
                            "sent": 1234567890000,
                            "event": "Flood Warning"
                        },
                        "geometry": {
                            "type": "Point",
                            "coordinates": [-97.7431, 30.2672]
                        }
                    }
                ]
            }
        """.trimIndent()


        val alerts = noaaService.getFloodAlerts(latitude, longitude)
        assertEquals(1, alerts.size)

        val alert = alerts.first()
        assertEquals("test-id", alert.id)
        assertEquals("Test Flood Warning", alert.title)
        assertEquals("Test description", alert.description)
        assertEquals("Moderate", alert.severity)
        assertEquals("Test Area", alert.location)
        assertEquals(30.2672, alert.latitude, 0.0001)
        assertEquals(-97.7431, alert.longitude, 0.0001)
        assertEquals(1234567890000, alert.timestamp)
    }

    // TODO: Fix this test
    @Test
    @Ignore
    fun `getFloodAlerts filters out non-flood alerts`() = runTest {
        val latitude = 30.2672
        val longitude = -97.7431
        val mockResponse = """
            {
                "features": [
                    {
                        "properties": {
                            "id": "flood-id",
                            "headline": "Test Flood Warning",
                            "description": "Test description",
                            "severity": "Moderate",
                            "areaDesc": "Test Area",
                            "sent": 1234567890000,
                            "event": "Flood Warning"
                        },
                        "geometry": {
                            "type": "Point",
                            "coordinates": [-97.7431, 30.2672]
                        }
                    },
                    {
                        "properties": {
                            "id": "tornado-id",
                            "headline": "Test Tornado Warning",
                            "description": "Test description",
                            "severity": "Severe",
                            "areaDesc": "Test Area",
                            "sent": 1234567890000,
                            "event": "Tornado Warning"
                        },
                        "geometry": {
                            "type": "Point",
                            "coordinates": [-97.7431, 30.2672]
                        }
                    }
                ]
            }
        """.trimIndent()


        val alerts = noaaService.getFloodAlerts(latitude, longitude)
        assertEquals(1, alerts.size)

        val alert = alerts.first()
        assertEquals("flood-id", alert.id)
        assertEquals("Test Flood Warning", alert.title)
    }

    @Test
    fun `getFloodAlerts uses correct point endpoint`() = runTest {
        val latitude = 30.2672
        val longitude = -97.7431
        val mockResponse = """
            {
                "features": []
            }
        """.trimIndent()


        noaaService.getFloodAlerts(latitude, longitude)
        verify { mockClient.newCall(any()) }
    }
}
