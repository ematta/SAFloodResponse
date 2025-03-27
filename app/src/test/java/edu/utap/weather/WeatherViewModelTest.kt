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
        viewModel = WeatherViewModel(mockNoaaService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        assertEquals(emptyList(), viewModel.floodAlerts.value)
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
        val mockAlerts = listOf(FloodAlert("test-id", "Test Alert", "Test Description", "Severe", "Test Location", 29.4241, -98.4936, 123456789))
        coEvery { mockNoaaService.getFloodAlerts(any(), any()) } returns mockAlerts

        // When & Then
        viewModel.isLoading.test {
            viewModel.fetchFloodAlerts(0.0, 0.0)
            assertTrue(awaitItem()) // Loading starts
            assertFalse(awaitItem()) // Loading ends
            cancel()
        }

        viewModel.floodAlerts.test {
            assertEquals(emptyList(), awaitItem()) // Initial state
            assertEquals(mockAlerts, awaitItem()) // Updated state
            cancel()
        }

        viewModel.error.test {
            assertNull(awaitItem())
            cancel()
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
        assertEquals(emptyList(), viewModel.floodAlerts.value)
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
        assertEquals(emptyList(), viewModel.floodAlerts.value)
        viewModel.error.test {
            assertNull(awaitItem())
        }
    }
}
