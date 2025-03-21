package edu.utap.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.firebase.auth.FirebaseUser
import edu.utap.auth.repository.AuthRepositoryInterface
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
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockAuthRepository: AuthRepositoryInterface
    private lateinit var authViewModel: AuthViewModel
    private lateinit var mockFirebaseUser: FirebaseUser

    private val testEmail = "test@example.com"
    private val testPassword = "password"
    private val testName = "Test User"

    @Before
    fun setup() = runTest {
        Dispatchers.setMain(testDispatcher)
        mockAuthRepository = mock(AuthRepositoryInterface::class.java)
        mockFirebaseUser = mock(FirebaseUser::class.java)
        // Setup the mock before creating the ViewModel
        whenever(mockAuthRepository.getCurrentUser()).thenReturn(null)
        authViewModel = AuthViewModel(mockAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        // Create a new ViewModel instance to test initial state
        val newViewModel = AuthViewModel(mockAuthRepository)
        assertEquals(AuthState.Idle, newViewModel.authState.value)
        
        // Then advance the test dispatcher to allow coroutines to complete
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Now check the state after initialization completes
        assertEquals(AuthState.Unauthenticated, newViewModel.authState.value)
    }

    @Test
    fun `login success sets Authenticated state`() = runTest {
        whenever(mockAuthRepository.loginUser(testEmail, testPassword))
            .thenReturn(Result.success(mockFirebaseUser))

        authViewModel.login(testEmail, testPassword)
        assertEquals(AuthState.Loading, authViewModel.authState.value)

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Authenticated)
    }

    @Test
    fun `login failure sets Error state`() = runTest {
        val errorMessage = "Invalid credentials"
        whenever(mockAuthRepository.loginUser(testEmail, testPassword))
            .thenReturn(Result.failure(Exception(errorMessage)))

        authViewModel.login(testEmail, testPassword)
        assertEquals(AuthState.Loading, authViewModel.authState.value)

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Error)
        assertEquals(errorMessage, (finalState as AuthState.Error).message)
    }

    @Test
    fun `register success sets Authenticated state`() = runTest {
        whenever(mockAuthRepository.registerUser(testEmail, testPassword, testName))
            .thenReturn(Result.success(mockFirebaseUser))

        authViewModel.register(testEmail, testPassword, testName)
        assertEquals(AuthState.Loading, authViewModel.authState.value)

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Authenticated)
    }

    @Test
    fun `register failure sets Error state`() = runTest {
        val errorMessage = "Registration failed"
        whenever(mockAuthRepository.registerUser(testEmail, testPassword, testName))
            .thenReturn(Result.failure(Exception(errorMessage)))

        authViewModel.register(testEmail, testPassword, testName)
        assertEquals(AuthState.Loading, authViewModel.authState.value)

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Error)
        assertEquals(errorMessage, (finalState as AuthState.Error).message)
    }

    @Test
    fun `logout sets Unauthenticated state`() = runTest {
        authViewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.Unauthenticated, authViewModel.authState.value)
    }
}