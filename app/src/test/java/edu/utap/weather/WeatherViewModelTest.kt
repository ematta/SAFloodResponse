package edu.utap.weather

import edu.utap.ui.viewmodel.WeatherViewModel
import edu.utap.user.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var viewModel: WeatherViewModel
    private lateinit var mockNoaaService: NOAAService

    @Before
    fun setup() {
        mockNoaaService = mockk()
        viewModel = WeatherViewModel(mockNoaaService)
    }

    @Test
    fun `initial state is empty`() = runTest {
        assertEquals(emptyList(), viewModel.floodAlerts.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }

    // TODO: Fix this test
    @Test
    @Ignore
    fun `fetchFloodAlerts updates state with alerts`() = runTest {
        // Given
        val mockAlerts = listOf(FloodAlert("test-id", "Test Alert", "Test Description", "Severe", "Test Location", 29.4241, -98.4936, 123456789))
        coEvery { mockNoaaService.getFloodAlerts(any(), any()) } returns mockAlerts

        // When
        viewModel.fetchFloodAlerts(0.0, 0.0)
        advanceUntilIdle() // Wait for coroutines to complete
        
        // Then
        assertFalse(viewModel.isLoading.value) // Loading should be false after completion
        assertEquals(mockAlerts, viewModel.floodAlerts.value) // Should have our mock alerts
        assertNull(viewModel.error.value) // No error should be present
    }

    // TODO: Fix this test
    @Test
    @Ignore
    fun `fetchFloodAlerts handles error`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { mockNoaaService.getFloodAlerts(any(), any()) } throws Exception(errorMessage)

        // When
        viewModel.fetchFloodAlerts(29.4241, -98.4936)
        advanceUntilIdle() // Wait for coroutines to complete

        // Then
        assertFalse(viewModel.isLoading.value) // Loading should be false after completion
        assertEquals(emptyList(), viewModel.floodAlerts.value) // Should be empty on error
        assertEquals("Failed to fetch flood alerts: $errorMessage", viewModel.error.value) // Error should be set
    }

    // TODO: Fix this test
    @Test
    @Ignore
    fun `fetchFloodAlerts handles empty alerts`() = runTest {
        // Given
        coEvery { mockNoaaService.getFloodAlerts(any(), any()) } returns emptyList()

        // When
        viewModel.fetchFloodAlerts(29.4241, -98.4936)
        advanceUntilIdle() // Wait for coroutines to complete

        // Then
        assertFalse(viewModel.isLoading.value) // Loading should be false after completion
        assertEquals(emptyList(), viewModel.floodAlerts.value) // Should still be empty
        assertNull(viewModel.error.value) // No error should be present
    }
}
