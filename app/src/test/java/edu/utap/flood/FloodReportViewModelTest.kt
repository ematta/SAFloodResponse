package edu.utap.flood

import android.location.Location
import edu.utap.auth.AuthViewModelInterface
import edu.utap.auth.db.UserEntity
import edu.utap.flood.model.FloodReport
import edu.utap.flood.repository.FloodReportRepositoryInterface
import edu.utap.flood.utils.LocationUtils
import edu.utap.user.MainDispatcherRule
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import com.google.android.gms.tasks.Task

@OptIn(ExperimentalCoroutinesApi::class)
class FloodReportViewModelTest {
    
    @get:Rule
    val testDispatcher = TestDispatcherRule()
    
    @MockK
    private lateinit var floodReportRepository: FloodReportRepositoryInterface
    
    @MockK
    private lateinit var authViewModel: AuthViewModelInterface
    
    @MockK
    private lateinit var locationUtils: LocationUtils
    
    private lateinit var viewModel: FloodReportViewModel
    
    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        // Explicitly mock the requestLocationUpdates method with any lambda parameter
        every { locationUtils.requestLocationUpdates(any()) } just runs
        viewModel = FloodReportViewModel(floodReportRepository, authViewModel, locationUtils)
    }
    
    @Test
    fun testUpdateDescription() {
        val description = "Test description"
        viewModel.updateDescription(description)
        assertEquals(description, viewModel.description.value)
    }
    
    @Test
    fun testAddPhoto() {
        val photoUrl = "test_photo.jpg"
        viewModel.addPhoto(photoUrl)
        assertEquals(listOf(photoUrl), viewModel.selectedPhotos.value)
    }
    
    @Test
    fun testRemovePhoto() = runTest {
        // Given
        val photoUrl = "https://example.com/photo.jpg"
        viewModel.addPhoto(photoUrl)
        
        // When
        viewModel.removePhoto(photoUrl)
        
        // Then
        assertEquals(emptyList<String>(), viewModel.selectedPhotos.value)
    }
    
    @Test
    fun testSetManualLocation() = runTest {
        // Given
        val enabled = true
        
        // When
        viewModel.setManualLocation(enabled)
        
        // Then
        assertEquals(enabled, viewModel.isManualLocation.value)
    }
    
    @Test
    fun testUpdateManualLocation() = runTest {
        // Given
        val latitude = 37.7749
        val longitude = -122.4194
        
        // When
        viewModel.updateManualLocation(latitude, longitude)
        
        // Then
        assertEquals(latitude, viewModel.manualLatitude.value)
        assertEquals(longitude, viewModel.manualLongitude.value)
    }
    
    @Test
    fun testSubmitReportSuccess() = runTest {
        // Given
        val mockUser = UserEntity(
            userId = "test-uid",
            email = "test@example.com",
            name = "Test User",
            role = "regular"
        )
        val mockLocation = mockk<Location>(relaxed = true)
        val description = "Test flood"
        val photoUrls = listOf("https://example.com/photo.jpg")
        
        every { mockLocation.latitude } returns 37.7749
        every { mockLocation.longitude } returns -122.4194
        every { authViewModel.getCurrentUser() } returns mockUser
        
        viewModel.updateDescription(description)
        photoUrls.forEach { viewModel.addPhoto(it) }
        
        // Mock location
        val mockTask = mockk<Task<Location>>(relaxed = true)
        every { mockTask.isSuccessful } returns true
        every { mockTask.result } returns mockLocation
        every { locationUtils.getLastKnownLocation() } returns mockTask
        
        // Mock repository
        coEvery { floodReportRepository.createReport(any()) } returns Result.success(
            FloodReport(
                reportId = "test-report-id",
                userId = mockUser.userId,
                latitude = 37.7749,
                longitude = -122.4194,
                description = description,
                photoUrls = photoUrls
            )
        )
        
        // When
        viewModel.submitReport()
        advanceUntilIdle() // Ensure all coroutines complete
        
        // Then
        assertEquals(ReportState.Success, viewModel.reportState.value)
        assertEquals("", viewModel.description.value)
        assertEquals(emptyList<String>(), viewModel.selectedPhotos.value)
        assertEquals(false, viewModel.isManualLocation.value)
        
        coVerify { floodReportRepository.createReport(any()) }
    }
    
    @Test
    fun testSubmitReportFailure() = runTest {
        // Given
        val mockUser = UserEntity(
            userId = "test-uid",
            email = "test@example.com",
            name = "Test User",
            role = "regular"
        )
        val mockLocation = mockk<Location>()
        val errorMessage = "Location not available"
        
        every { authViewModel.getCurrentUser() } returns mockUser
        every { mockLocation.latitude } returns 37.7749
        every { mockLocation.longitude } returns -122.4194
        
        // Mock location
        every { locationUtils.getLastKnownLocation() } returns mockk<Task<Location>> {
            every { isSuccessful } returns true
            every { result } returns mockLocation
        }
        
        // Mock repository failure
        coEvery { floodReportRepository.createReport(any()) } returns Result.failure(
            Exception(errorMessage)
        )
        
        // When
        viewModel.submitReport()
        advanceUntilIdle() // Ensure all coroutines complete
        
        // Then
        assertEquals(errorMessage, (viewModel.reportState.value as ReportState.Error).message)
    }
    
    @Test
    fun testConfirmReport() = runTest {
        // Given
        val reportId = "test-report-id"
        val mockReport = FloodReport(
            reportId = reportId,
            userId = "test-uid",
            latitude = 37.7749,
            longitude = -122.4194,
            description = "Test flood",
            confirmedCount = 0
        )
        
        coEvery { floodReportRepository.getReportById(reportId) } returns Result.success(mockReport)
        coEvery { floodReportRepository.updateReport(any()) } returns Result.success(
            mockReport.copy(confirmedCount = 1)
        )
        
        // When
        viewModel.confirmReport(reportId)
        
        // Then
        coVerify { floodReportRepository.updateReport(any()) }
    }
    
    @Test
    fun testDenyReport() = runTest {
        // Given
        val reportId = "test-report-id"
        val mockReport = FloodReport(
            reportId = reportId,
            userId = "test-uid",
            latitude = 37.7749,
            longitude = -122.4194,
            description = "Test flood",
            deniedCount = 0
        )
        
        coEvery { floodReportRepository.getReportById(reportId) } returns Result.success(mockReport)
        coEvery { floodReportRepository.updateReport(any()) } returns Result.success(
            mockReport.copy(deniedCount = 1)
        )
        
        // When
        viewModel.denyReport(reportId)
        
        // Then
        coVerify { floodReportRepository.updateReport(any()) }
    }
}

class TestDispatcherRule : TestRule {
    private val testDispatcher = StandardTestDispatcher()
    override fun apply(base: org.junit.runners.model.Statement, description: org.junit.runner.Description) =
        object : org.junit.runners.model.Statement() {
            override fun evaluate() {
                kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
                try {
                    base.evaluate()
                } finally {
                    kotlinx.coroutines.Dispatchers.resetMain()
                }
            }
        }
}
