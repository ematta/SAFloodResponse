package edu.utap.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import edu.utap.FloodResponseApplication
import edu.utap.auth.UserProfileState
import edu.utap.ui.viewmodel.UserViewModel
import edu.utap.repository.UserRepository
import edu.utap.auth.MainDispatcherRule
import edu.utap.models.UserProfile
import edu.utap.utils.FirebaseStorageUtil
import edu.utap.utils.NetworkUtils
import edu.utap.utils.NetworkUtilsImpl
import edu.utap.utils.NetworkUtilsInterface
import edu.utap.utils.LocationUtils
import kotlin.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class UserViewModelProfileImageTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    // Mock dependencies
    @MockK
    private lateinit var mockUserRepository: UserRepository
    
    @MockK
    private lateinit var mockStorageUtil: FirebaseStorageUtil
    
    @MockK
    private lateinit var mockContext: Context
    
    @MockK
    private lateinit var mockUri: Uri

    @MockK
    private lateinit var mockApplication: FloodResponseApplication
    
    @MockK
    private lateinit var mockNetworkUtils: NetworkUtilsInterface

    @MockK
    private lateinit var mockLocationUtils: LocationUtils
    
    // Class under test
    private lateinit var userViewModel: UserViewModel
    
    // Test data
    private val testUid = "test-uid"
    private val testDownloadUrl = "https://example.com/test-image.jpg"
    private val testUserProfile = UserProfile(
        uid = testUid,
        displayName = "Test User",
        email = "test@user.com",
        photoUrl = "",
        phoneNumber = "123-456-7890",
        address = "123 Test St"
    )
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // Make network always available for tests
        every { mockNetworkUtils.isNetworkAvailable(any()) } returns true
        // Set the mock implementation
        NetworkUtils.setImplementation(mockNetworkUtils)
        // Stub application context on mockContext
        every { mockContext.getApplicationContext() } returns mockContext
        every { mockContext.applicationContext } returns mockContext
        // Stub application context on mockApplication
        every { mockApplication.getApplicationContext() } returns mockContext
        every { mockApplication.applicationContext } returns mockContext
        // Now inject LocationUtils
        every { mockLocationUtils.hasLocationPermissions() } returns true
        coEvery { mockLocationUtils.getCurrentLocation() } returns null
        
        userViewModel = UserViewModel(
            userRepository = mockUserRepository,
            storageUtil = mockStorageUtil,
            application = mockApplication,
            locationUtils = mockLocationUtils
        )
    }
    
    @After
    fun tearDown() {
        // Restore default implementation
        NetworkUtils.setImplementation(NetworkUtilsImpl())
        
        // Reset ApplicationContextProvider mock
    }
    
    @Test
    fun `uploadProfileImage updates state to Loading then calls storageUtil`() = runTest {
        // Arrange
        val initialState = UserProfileState.Idle.Initial
        // viewModel already created with injected locationUtils
        assertTrue(userViewModel.profileState.value is UserProfileState.Idle, "Initial state should be Idle")
        
        coEvery { mockStorageUtil.uploadProfileImage(any(), any(), any()) } returns Result.success(testDownloadUrl)
        coEvery { mockUserRepository.updatePhotoUrl(any(), any()) } returns Result.success(Unit)
        coEvery { mockUserRepository.getUserProfile(any()) } returns Result.success(testUserProfile)
            
        // Act
        userViewModel.uploadProfileImage(mockContext, mockUri, testUid)
        
        // Assert - verify storageUtil was called
        advanceUntilIdle()
        coVerify { mockStorageUtil.uploadProfileImage(mockContext, mockUri, testUid) }
    }
    
    @Test
    fun `uploadProfileImage updates photoUrl on successful upload`() = runTest {
        // Arrange
        coEvery { mockStorageUtil.uploadProfileImage(mockContext, mockUri, testUid) } returns Result.success(testDownloadUrl)
        coEvery { mockUserRepository.updatePhotoUrl(testUid, testDownloadUrl) } returns Result.success(Unit)
        coEvery { mockUserRepository.getUserProfile(testUid) } returns Result.success(testUserProfile.copy(photoUrl = testDownloadUrl))
        
        // Act
        userViewModel.uploadProfileImage(mockContext, mockUri, testUid)
        advanceUntilIdle()
        
        // Assert
        coVerify { mockStorageUtil.uploadProfileImage(mockContext, mockUri, testUid) }
        coVerify { mockUserRepository.updatePhotoUrl(testUid, testDownloadUrl) }
        coVerify { mockUserRepository.getUserProfile(testUid) }
        
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Success)
        assertEquals(testDownloadUrl, (state as UserProfileState.Success).profile.photoUrl)
    }
    
    @Test
    fun `uploadProfileImage sets Error state when upload fails`() = runTest {
        // Arrange
        val errorMessage = "Upload failed"
        val exception = Exception(errorMessage)
        val failure = Result.failure<String>(exception)
        coEvery { mockStorageUtil.uploadProfileImage(mockContext, mockUri, testUid) } returns failure
        
        // Act
        userViewModel.uploadProfileImage(mockContext, mockUri, testUid)
        advanceUntilIdle()
        
        // Assert
        coVerify { mockStorageUtil.uploadProfileImage(mockContext, mockUri, testUid) }
        
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Error)
        assertContains(
            (state as UserProfileState.Error.Generic).message.toString(),
            (failure.exceptionOrNull() as Exception).message.toString(),
            ignoreCase = true
        )
    }
    
    @Test
    fun `uploadProfileImage sets Error state when updatePhotoUrl fails`() = runTest {
        // Arrange
        val errorMessage = "Failed to update photo URL"
        val exception = Exception(errorMessage)
        val failure = Result.failure<String>(exception)

        coEvery { mockStorageUtil.uploadProfileImage(mockContext, mockUri, testUid) } returns failure
        coEvery { mockUserRepository.updatePhotoUrl(testUid, testDownloadUrl) } returns Result.failure(exception)
        
        // Act
        userViewModel.uploadProfileImage(mockContext, mockUri, testUid)
        advanceUntilIdle()
        
        // Assert
        coVerify { mockStorageUtil.uploadProfileImage(mockContext, mockUri, testUid) }

        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Error)
        assertContains(
            (state as UserProfileState.Error.Generic).message,
            errorMessage,
            ignoreCase = true
        )
    }
    
    @Test
    fun `uploadProfileImage handles complete flow from upload to profile refresh`() = runTest {
        // Arrange
        val updatedProfile = testUserProfile.copy(photoUrl = testDownloadUrl)
        coEvery { mockStorageUtil.uploadProfileImage(mockContext, mockUri, testUid) } returns Result.success(testDownloadUrl)
        coEvery { mockUserRepository.updatePhotoUrl(testUid, testDownloadUrl) } returns Result.success(Unit)
        coEvery { mockUserRepository.getUserProfile(testUid) } returns Result.success(updatedProfile)
        
        // Act
        userViewModel.uploadProfileImage(mockContext, mockUri, testUid)
        advanceUntilIdle()
        
        // Assert - Verify the complete flow
        coVerify { mockStorageUtil.uploadProfileImage(mockContext, mockUri, testUid) }
        coVerify { mockUserRepository.updatePhotoUrl(testUid, testDownloadUrl) }
        coVerify { mockUserRepository.getUserProfile(testUid) }
        
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Success)
        assertEquals(updatedProfile, (state as UserProfileState.Success).profile)
    }
}
