package edu.utap.weather

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    private lateinit var viewModel: WeatherViewModel
    private lateinit var mockNoaaService: NOAAService
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockNoaaService = mockk()
        viewModel = WeatherViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        viewModel.floodAlerts.test {
            assertEquals(emptyList(), awaitItem())
        }
        viewModel.isLoading.test {
            assertFalse(awaitItem())
        }
        viewModel.error.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `fetchFloodAlerts updates state with alerts`() = runTest {
        // Given
        val testAlerts = listOf(
            FloodAlert(
                id = "test-id",
                title = "Test Alert",
                description = "Test Description",
                severity = "Severe",
                location = "Test Location",
                latitude = 29.4241,
                longitude = -98.4936,
                timestamp = 1234567890L
            )
        )
        coEvery { mockNoaaService.getFloodAlerts(29.4241, -98.4936) } returns testAlerts

        // When
        viewModel.fetchFloodAlerts(29.4241, -98.4936)

        // Then
        viewModel.isLoading.test {
            assertTrue(awaitItem()) // Loading starts
            assertFalse(awaitItem()) // Loading ends
        }
        viewModel.floodAlerts.test {
            assertEquals(testAlerts, awaitItem())
        }
        viewModel.error.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `fetchFloodAlerts handles error`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { mockNoaaService.getFloodAlerts(29.4241, -98.4936) } throws Exception(errorMessage)

        // When
        viewModel.fetchFloodAlerts(29.4241, -98.4936)

        // Then
        viewModel.isLoading.test {
            assertTrue(awaitItem()) // Loading starts
            assertFalse(awaitItem()) // Loading ends
        }
        viewModel.floodAlerts.test {
            assertEquals(emptyList(), awaitItem())
        }
        viewModel.error.test {
            assertEquals("Failed to fetch flood alerts: $errorMessage", awaitItem())
        }
    }

    @Test
    fun `fetchFloodAlerts handles empty alerts`() = runTest {
        // Given
        coEvery { mockNoaaService.getFloodAlerts(29.4241, -98.4936) } returns emptyList()

        // When
        viewModel.fetchFloodAlerts(29.4241, -98.4936)

        // Then
        viewModel.isLoading.test {
            assertTrue(awaitItem()) // Loading starts
            assertFalse(awaitItem()) // Loading ends
        }
        viewModel.floodAlerts.test {
            assertEquals(emptyList(), awaitItem())
        }
        viewModel.error.test {
            assertNull(awaitItem())
        }
    }
} 