package edu.utap.weather

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WeatherRepositoryImplTest {
    private lateinit var noaaService: NOAAService
    private lateinit var repository: WeatherRepositoryImpl

    @Before
    fun setUp() {
        noaaService = mockk()
        repository = WeatherRepositoryImpl(noaaService)
    }

    @Test
    fun `getFloodAlerts returns expected data`() = runBlocking {
        // Arrange
        val expectedAlerts = listOf(FloodAlert(id = "test-id", title = "Test Alert", description = "Test Description", severity = "Severe", location = "Test Location", latitude = 0.0, longitude = 0.0, timestamp = 1234567890L))
        coEvery { noaaService.getFloodAlerts(any(), any()) } returns expectedAlerts

        // Act
        val result = repository.getFloodAlerts(0.0, 0.0)

        // Assert
        assertEquals(expectedAlerts, result)
        coVerify(exactly = 1) { noaaService.getFloodAlerts(0.0, 0.0) }
    }
}