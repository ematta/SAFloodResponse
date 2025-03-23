package edu.utap.user

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import android.content.Context
import io.mockk.*
import io.mockk.impl.annotations.MockK
import edu.utap.auth.utils.NetworkUtils
import edu.utap.auth.utils.NetworkUtilsInterface
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        email = "test@example.com",
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
        
        userViewModel = UserViewModel(userRepository, storageUtil, mockContext)
    }

    @Test
    fun `getUserProfile should update state to Success with profile on successful repository call`() = runTest {
        // Given
        coEvery { userRepository.getUserProfile(testUid) } returns Result.success(testUserProfile)

        // When
        userViewModel.getUserProfile(testUid)

        // Then
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Success)
        assertEquals(testUserProfile, (state as UserProfileState.Success).userProfile)
        coVerify { userRepository.getUserProfile(testUid) }
    }

    @Test
    fun `getUserProfile should update state to Error on repository failure`() = runTest {
        // Given
        val errorMessage = "Profile not found"
        coEvery { userRepository.getUserProfile(testUid) } returns Result.failure(Exception(errorMessage))

        // When
        userViewModel.getUserProfile(testUid)

        // Then
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Error)
        assertEquals(errorMessage, (state as UserProfileState.Error).message)
        coVerify { userRepository.getUserProfile(testUid) }
    }

    @Test
    fun `createUserProfile should update state to Success with profile on successful repository call`() = runTest {
        // Given
        coEvery { userRepository.createUserProfile(testUserProfile) } returns Result.success(testUserProfile)

        // When
        userViewModel.createUserProfile(testUserProfile)

        // Then
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Success)
        assertEquals(testUserProfile, (state as UserProfileState.Success).userProfile)
        coVerify { userRepository.createUserProfile(testUserProfile) }
    }

    @Test
    fun `updateUserProfile should update state to Success with profile on successful repository call`() = runTest {
        // Given
        coEvery { userRepository.updateUserProfile(testUserProfile) } returns Result.success(testUserProfile)

        // When
        userViewModel.updateUserProfile(testUserProfile)

        // Then
        val state = userViewModel.profileState.value
        assertTrue(state is UserProfileState.Success)
        assertEquals(testUserProfile, (state as UserProfileState.Success).userProfile)
        coVerify { userRepository.updateUserProfile(testUserProfile) }
    }

    @Test
    fun `updateDisplayName should call repository and then refresh profile on success`() = runTest {
        // Given
        val displayName = "Updated Name"
        coEvery { userRepository.updateDisplayName(testUid, displayName) } returns Result.success(Unit)
        coEvery { userRepository.getUserProfile(testUid) } returns Result.success(testUserProfile)

        // When
        userViewModel.updateDisplayName(testUid, displayName)

        // Then
        coVerify { userRepository.updateDisplayName(testUid, displayName) }
        coVerify { userRepository.getUserProfile(testUid) }
    }

    @Test
    fun `updatePhotoUrl should call repository and then refresh profile on success`() = runTest {
        // Given
        val photoUrl = "https://example.com/photo.jpg"
        coEvery { userRepository.updatePhotoUrl(testUid, photoUrl) } returns Result.success(Unit)
        coEvery { userRepository.getUserProfile(testUid) } returns Result.success(testUserProfile)

        // When
        userViewModel.updatePhotoUrl(testUid, photoUrl)

        // Then
        coVerify { userRepository.updatePhotoUrl(testUid, photoUrl) }
        coVerify { userRepository.getUserProfile(testUid) }
    }
}