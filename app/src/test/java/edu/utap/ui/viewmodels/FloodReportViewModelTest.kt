@file:OptIn(ExperimentalCoroutinesApi::class)

package edu.utap.ui.viewmodels

import android.location.Location
import com.google.android.gms.tasks.Task
import edu.utap.models.FirestoreUser
import edu.utap.models.FloodReport
import edu.utap.repository.FloodReportRepositoryInterface
import edu.utap.ui.viewmodel.AuthViewModelInterface
import edu.utap.utils.LocationUtils
import edu.utap.ui.viewmodel.FloodReportViewModel
import edu.utap.ui.viewmodel.ReportState
import edu.utap.utils.FirebaseStorageUtilInterface
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import edu.utap.repository.DiscussionRepositoryInterface
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class FloodReportViewModelTest {
    
    @get:Rule
    val testDispatcher = TestDispatcherRule()
    
    @MockK
    private lateinit var floodReportRepository: FloodReportRepositoryInterface

    @MockK
    private lateinit var storageUtil: FirebaseStorageUtilInterface
    
    @MockK
    private lateinit var authViewModel: AuthViewModelInterface
    
    @MockK
    private lateinit var locationUtils: LocationUtils
    
    @MockK
    private lateinit var discussionRepository: DiscussionRepositoryInterface
    
    private lateinit var viewModel: FloodReportViewModel
    
    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        // Mock default user to prevent unconfigured getCurrentUser() calls during init
        every { authViewModel.getCurrentUser() } returns null
        // Explicitly mock the requestLocationUpdates method with any lambda parameter
        every { locationUtils.requestLocationUpdates(any()) } just runs
        // Mock discussion thread creation to avoid Firebase calls
        coEvery { discussionRepository.createThread(any()) } returns Result.success(
            mockk(relaxed = true)
        )
        viewModel = FloodReportViewModel(
            floodReportRepository, authViewModel, locationUtils,
            storageUtil = storageUtil,
            discussionRepository = discussionRepository
        )
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
    fun testSubmitReportSuccess() = runTest {
        // Given
        val mockUser = mockk<FirestoreUser>(relaxed = true)
        every { mockUser.userId } returns "test-uid"
        every { mockUser.email } returns "test@user.com"
        every { mockUser.name } returns "Test User"
        // Note: FirebaseUser does not have a 'role' property.
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
        // No need to mock getLastKnownLocation if using manual location
        // every { locationUtils.getLastKnownLocation() } returns mockTask

        // Enable manual location for the test
        viewModel.setManualLocation(true)
        viewModel.updateManualLocation(37.7749, -122.4194)

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
        val mockUser = mockk<FirestoreUser>(relaxed = true)
        every { mockUser.userId } returns "test-uid"
        every { mockUser.email } returns "test@user.com"
        every { mockUser.name } returns "Test User"
        // Note: FirebaseUser does not have a 'role' property.
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
        // Avoid Tasks.await main-thread exception by using manual location
        viewModel.setManualLocation(true)
        viewModel.updateManualLocation(37.7749, -122.4194)

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
        advanceUntilIdle() // Ensure coroutine completes

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
        advanceUntilIdle() // Ensure coroutine completes

        // Then
        coVerify { floodReportRepository.updateReport(any()) }
    }
}

class TestDispatcherRule : TestRule {
    private val testDispatcher = StandardTestDispatcher()
    override fun apply(base: Statement, description: Description) =
        object : Statement() {
            override fun evaluate() {
                Dispatchers.setMain(testDispatcher)
                try {
                    base.evaluate()
                } finally {
                    Dispatchers.resetMain()
                }
            }
        }
}
