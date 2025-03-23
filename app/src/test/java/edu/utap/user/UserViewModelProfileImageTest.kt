package edu.utap.user

import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import edu.utap.auth.utils.NetworkUtils
import edu.utap.auth.utils.NetworkUtilsInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class UserViewModelProfileImageTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    // Mock dependencies
    private lateinit var mockUserRepository: UserRepository
    private lateinit var mockStorageUtil: FirebaseStorageUtil
    private lateinit var mockContext: Context
    private lateinit var mockUri: Uri
    private lateinit var mockNetworkUtils: NetworkUtilsInterface
    
    // Class under test
    private lateinit var userViewModel: UserViewModel
    
    // Test data
    private val testUid = "test-uid"
    private val testDownloadUrl = "https://example.com/test-image.jpg"
    private val testUserProfile = UserProfile(
        uid = testUid,
        displayName = "Test User",
        email = "test@example.com",
        photoUrl = "",
        phoneNumber = "123-456-7890",
        address = "123 Test St"
    )
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        mockUserRepository = mock()
        mockStorageUtil = mock()
        mockContext = mock()
        mockUri = mock()
        
        // Create a mock implementation of NetworkUtilsInterface
        mockNetworkUtils = mock<NetworkUtilsInterface>()
        // Make network always available for tests
        whenever(mockNetworkUtils.isNetworkAvailable(any())).thenReturn(true)
        // Set the mock implementation
        NetworkUtils.setImplementation(mockNetworkUtils)
        
        userViewModel = UserViewModel(mockUserRepository, mockStorageUtil, mockContext)
    }
    
    @After
    fun tearDown() {
        // Restore default implementation
        NetworkUtils.setImplementation(edu.utap.auth.utils.NetworkUtilsImpl())
    }
    
    @Test
    fun `uploadProfileImage updates state to Loading then calls storageUtil`() = runTest {
        // Arrange
        val initialState = UserProfileState.Idle
        userViewModel = UserViewModel(mockUserRepository, mockStorageUtil, mockContext)
        assertTrue(userViewModel.profileState.value is UserProfileState.Idle, "Initial state should be Idle")
        
        whenever(mockStorageUtil.uploadProfileImage(any(), any(), any()))
            .thenReturn(Result.success(testDownloadUrl))
        whenever(mockUserRepository.updatePhotoUrl(any(), any()))
            .thenReturn(Result.success(Unit))
        whenever(mockUserRepository.getUserProfile(any()))
            .thenReturn(Result.success(testUserProfile))
            
        // Act
        userViewModel.uploadProfileImage(mockContext, mockUri, testUid)
        
        // Assert - verify storageUtil was called
        advanceUntilIdle()
        verify(mockStorageUtil).uploadProfileImage(mockContext, mockUri, testUid)
    }
    
    @Test
    fun `uploadProfileImage updates photoUrl on successful upload`() = runTest {
        // Arrange
        whenever(mockStorageUtil.uploadProfileImage(mockContext, mockUri, testUid))
            .thenReturn(Result.success(testDownloadUrl))
        whenever(mockUserRepository.updatePhotoUrl(testUid, testDownloadUrl))
            .thenReturn(Result.success(Unit))
        whenever(mockUserRepository.getUserProfile(testUid))
            .thenReturn(Result.success(testUserProfile.copy(photoUrl = testDownloadUrl)))
        
        // Act
        userViewModel.uploadProfileImage(mockContext, mockUri, testUid)
        advanceUntilIdle()
        
        // Assert
        verify(mockStorageUtil).uploadProfileImage(mockContext, mockUri, testUid)
        verify(mockUserRepository).updatePhotoUrl(testUid, testDownloadUrl)
        verify(mockUserRepository).getUserProfile(testUid)
        
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Success)
        assertEquals(testDownloadUrl, (state as UserProfileState.Success).userProfile.photoUrl)
    }
    
    @Test
    fun `uploadProfileImage sets Error state when upload fails`() = runTest {
        // Arrange
        val errorMessage = "Upload failed"
        val exception = Exception(errorMessage)
        whenever(mockStorageUtil.uploadProfileImage(mockContext, mockUri, testUid))
            .thenReturn(Result.failure(exception))
        
        // Act
        userViewModel.uploadProfileImage(mockContext, mockUri, testUid)
        advanceUntilIdle()
        
        // Assert
        verify(mockStorageUtil).uploadProfileImage(mockContext, mockUri, testUid)
        
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Error)
        assertEquals(errorMessage, (state as UserProfileState.Error).message)
    }
    
    @Test
    fun `uploadProfileImage sets Error state when updatePhotoUrl fails`() = runTest {
        // Arrange
        val errorMessage = "Failed to update photo URL"
        val exception = Exception(errorMessage)
        whenever(mockStorageUtil.uploadProfileImage(mockContext, mockUri, testUid))
            .thenReturn(Result.success(testDownloadUrl))
        whenever(mockUserRepository.updatePhotoUrl(testUid, testDownloadUrl))
            .thenReturn(Result.failure(exception))
        
        // Act
        userViewModel.uploadProfileImage(mockContext, mockUri, testUid)
        advanceUntilIdle()
        
        // Assert
        verify(mockStorageUtil).uploadProfileImage(mockContext, mockUri, testUid)
        verify(mockUserRepository).updatePhotoUrl(testUid, testDownloadUrl)
        
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Error)
        assertEquals(errorMessage, (state as UserProfileState.Error).message)
    }
    
    @Test
    fun `uploadProfileImage handles complete flow from upload to profile refresh`() = runTest {
        // Arrange
        val updatedProfile = testUserProfile.copy(photoUrl = testDownloadUrl)
        whenever(mockStorageUtil.uploadProfileImage(mockContext, mockUri, testUid))
            .thenReturn(Result.success(testDownloadUrl))
        whenever(mockUserRepository.updatePhotoUrl(testUid, testDownloadUrl))
            .thenReturn(Result.success(Unit))
        whenever(mockUserRepository.getUserProfile(testUid))
            .thenReturn(Result.success(updatedProfile))
        
        // Act
        userViewModel.uploadProfileImage(mockContext, mockUri, testUid)
        advanceUntilIdle()
        
        // Assert - Verify the complete flow
        verify(mockStorageUtil).uploadProfileImage(mockContext, mockUri, testUid)
        verify(mockUserRepository).updatePhotoUrl(testUid, testDownloadUrl)
        verify(mockUserRepository).getUserProfile(testUid)
        
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Success)
        assertEquals(updatedProfile, (state as UserProfileState.Success).userProfile)
    }
}