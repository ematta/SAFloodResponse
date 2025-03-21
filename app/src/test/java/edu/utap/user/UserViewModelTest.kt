package edu.utap.user

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class UserViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var userRepository: UserRepository

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
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        userViewModel = UserViewModel(userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getUserProfile should update state to Success with profile on successful repository call`() = runTest {
        // Given
        `when`(userRepository.getUserProfile(testUid)).thenReturn(Result.success(testUserProfile))

        // When
        userViewModel.getUserProfile(testUid)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = userViewModel.profileState.first()
        assertTrue(state is UserProfileState.Success)
        assertEquals(testUserProfile, (state as UserProfileState.Success).userProfile)
        verify(userRepository).getUserProfile(testUid)
    }

    @Test
    fun `getUserProfile should update state to Error on repository failure`() = runTest {
        // Given
        val errorMessage = "Profile not found"
        `when`(userRepository.getUserProfile(testUid)).thenReturn(Result.failure(Exception(errorMessage)))

        // When
        userViewModel.getUserProfile(testUid)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = userViewModel.profileState.first()
        assertTrue(state is UserProfileState.Error)
        assertEquals(errorMessage, (state as UserProfileState.Error).message)
        verify(userRepository).getUserProfile(testUid)
    }

    @Test
    fun `createUserProfile should update state to Success with profile on successful repository call`() = runTest {
        // Given
        `when`(userRepository.createUserProfile(testUserProfile)).thenReturn(Result.success(testUserProfile))

        // When
        userViewModel.createUserProfile(testUserProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = userViewModel.profileState.first()
        assertTrue(state is UserProfileState.Success)
        assertEquals(testUserProfile, (state as UserProfileState.Success).userProfile)
        verify(userRepository).createUserProfile(testUserProfile)
    }

    @Test
    fun `updateUserProfile should update state to Success with profile on successful repository call`() = runTest {
        // Given
        `when`(userRepository.updateUserProfile(testUserProfile)).thenReturn(Result.success(testUserProfile))

        // When
        userViewModel.updateUserProfile(testUserProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = userViewModel.profileState.first()
        assertTrue(state is UserProfileState.Success)
        assertEquals(testUserProfile, (state as UserProfileState.Success).userProfile)
        verify(userRepository).updateUserProfile(testUserProfile)
    }

    @Test
    fun `updateDisplayName should call repository and then refresh profile on success`() = runTest {
        // Given
        val displayName = "Updated Name"
        `when`(userRepository.updateDisplayName(testUid, displayName)).thenReturn(Result.success(Unit))
        `when`(userRepository.getUserProfile(testUid)).thenReturn(Result.success(testUserProfile))

        // When
        userViewModel.updateDisplayName(testUid, displayName)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(userRepository).updateDisplayName(testUid, displayName)
        verify(userRepository).getUserProfile(testUid)
    }

    @Test
    fun `updatePhotoUrl should call repository and then refresh profile on success`() = runTest {
        // Given
        val photoUrl = "https://example.com/photo.jpg"
        `when`(userRepository.updatePhotoUrl(testUid, photoUrl)).thenReturn(Result.success(Unit))
        `when`(userRepository.getUserProfile(testUid)).thenReturn(Result.success(testUserProfile))

        // When
        userViewModel.updatePhotoUrl(testUid, photoUrl)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(userRepository).updatePhotoUrl(testUid, photoUrl)
        verify(userRepository).getUserProfile(testUid)
    }
} 