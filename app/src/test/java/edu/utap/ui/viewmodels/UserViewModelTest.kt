package edu.utap.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import android.content.Context
import android.net.Uri
import edu.utap.auth.UserProfileState
import edu.utap.ui.viewmodel.UserViewModel
import edu.utap.repository.UserRepository
import edu.utap.auth.MainDispatcherRule
import edu.utap.models.UserProfile
import edu.utap.utils.FirebaseStorageUtil
import io.mockk.*
import io.mockk.impl.annotations.MockK
import edu.utap.utils.NetworkUtils
import edu.utap.utils.NetworkUtilsImpl
import edu.utap.utils.NetworkUtilsInterface
import edu.utap.utils.Result
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import java.io.File
import java.io.IOException

@ExperimentalCoroutinesApi
class UserViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var userRepository: UserRepository
    
    @MockK
    private lateinit var storageUtil: FirebaseStorageUtil
    
    @MockK
    private lateinit var mockContext: Context
    
    @MockK
    private lateinit var mockNetworkUtils: NetworkUtilsInterface

    private lateinit var userViewModel: UserViewModel

    private val testUid = "test-uid"
    private val testUserProfile = UserProfile(
        uid = testUid,
        displayName = "Test User",
        email = "test@user.com",
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
        
        // Mock ApplicationContextProvider to return mockContext

        userViewModel = UserViewModel(userRepository, storageUtil)
    }

    @Test
    fun `getUserProfile should update state to Success with profile on successful repository call`() = runTest {
        // Given
        coEvery { userRepository.getUserProfile(testUid) } returns Result.Success(testUserProfile)

        // When
        userViewModel.getUserProfile(testUid)

        // Then
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Success)
        assertEquals(testUserProfile, (state as UserProfileState.Success).profile)
        coVerify { userRepository.getUserProfile(testUid) }
    }

    @Test
    fun `getUserProfile should update state to Error on repository failure`() = runTest {
        // Given
        val errorMessage = "Profile not found"
        coEvery { userRepository.getUserProfile(testUid) } returns Result.Error(errorMessage, Exception(errorMessage))

        // When
        userViewModel.getUserProfile(testUid)

        // Then
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Error)
        assertEquals(errorMessage, (state as UserProfileState.Error.Generic).message)
        coVerify { userRepository.getUserProfile(testUid) }
    }

    @Test
    fun `createUserProfile should update state to Success with profile on successful repository call`() = runTest {
        // Given
        coEvery { userRepository.createUserProfile(testUserProfile) } returns Result.Success(testUserProfile)

        // When
        userViewModel.createUserProfile(testUserProfile)

        // Then
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Success)
        assertEquals(testUserProfile, (state as UserProfileState.Success).profile)
        coVerify { userRepository.createUserProfile(testUserProfile) }
    }

    @Test
    fun `updateUserProfile should update state to Success with profile on successful repository call`() = runTest {
        // Given
        coEvery { userRepository.updateUserProfile(testUserProfile) } returns Result.Success(testUserProfile)

        // When
        userViewModel.updateUserProfile(testUserProfile)

        // Then
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Success)
        assertEquals(testUserProfile, (state as UserProfileState.Success).profile)
        coVerify { userRepository.updateUserProfile(testUserProfile) }
    }

    @Test
    fun `updateDisplayName should call repository and then refresh profile on success`() = runTest {
        // Given
        val displayName = "Updated Name"
        coEvery { userRepository.updateDisplayName(testUid, displayName) } returns Result.Success(Unit)
        coEvery { userRepository.getUserProfile(testUid) } returns Result.Success(testUserProfile)

        // When
        userViewModel.updateDisplayName(testUid, displayName)

        // Then
        coVerify { userRepository.updateDisplayName(testUid, displayName) }
        coVerify { userRepository.getUserProfile(testUid) }
    }

    @Test
    fun `uploadProfileImage should call repository and then refresh profile on success updatesState`() = runTest {
        val testUri = mockk<Uri>(relaxed = true)
        val testFile = mockk<File>(relaxed = true)
        coEvery { storageUtil.uploadProfileImage(any(), any(), any()) } returns Result.Success("https://test.com/image.jpg")
        coEvery { userRepository.updatePhotoUrl(any(), any()) } returns Result.Success(Unit)
        coEvery { userRepository.getUserProfile(any()) } returns Result.Success(
            UserProfile(
                uid = testUid,
                photoUrl = "https://test.com/image.jpg"
            )
        )

        userViewModel.uploadProfileImage(mockContext, testUri, testUid)
        advanceUntilIdle()

        // Verify state transitions
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Success)
        assertEquals("https://test.com/image.jpg", (state as UserProfileState.Success).profile.photoUrl)

        coVerify { storageUtil.uploadProfileImage(mockContext, testUri, testUid) }
        assertEquals((userViewModel.profileState.value as UserProfileState.Success).profile.photoUrl, "https://test.com/image.jpg")
    }

    @Test
    fun `uploadProfileImage with failure setsError`() = runTest {
        val testUri = mockk<Uri>(relaxed = true)
        coEvery { storageUtil.uploadProfileImage(any(), any(), any()) } returns Result.Error("Upload failed", IOException("Upload failed"))

        userViewModel.uploadProfileImage(mockContext, testUri, testUid)
        advanceUntilIdle()

        // Verify state transitions
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Error)

        assertEquals((userViewModel.profileState.value as UserProfileState.Error.Generic).message,
            "Upload failed")
    }

    @After
    fun tearDown() {
        // Restore default implementation
        NetworkUtils.setImplementation(NetworkUtilsImpl())
        
        // Reset ApplicationContextProvider mock
        unmockkAll()
    }

    @Test
    fun `updatePhotoUrl should call repository and then refresh profile on success`() = runTest {
        // Given
        val photoUrl = "https://example.com/photo.jpg"
        coEvery { userRepository.updatePhotoUrl(testUid, photoUrl) } returns Result.Success(Unit)
        coEvery { userRepository.getUserProfile(testUid) } returns Result.Success(testUserProfile)

        // When
        userViewModel.updatePhotoUrl(testUid, photoUrl)

        // Then
        coVerify { userRepository.updatePhotoUrl(testUid, photoUrl) }
        coVerify { userRepository.getUserProfile(testUid) }
    }
}
