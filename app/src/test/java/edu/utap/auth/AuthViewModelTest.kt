package edu.utap.auth

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.firebase.auth.FirebaseUser
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.auth.utils.NetworkUtils
import edu.utap.auth.utils.NetworkUtilsInterface
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
import io.mockk.*
import io.mockk.impl.annotations.MockK

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    @MockK
    private lateinit var mockAuthRepository: AuthRepositoryInterface
    
    @MockK
    private lateinit var mockContext: Context
    
    @MockK
    private lateinit var mockNetworkUtils: NetworkUtilsInterface
    
    private lateinit var authViewModel: AuthViewModel
    
    @MockK
    private lateinit var mockFirebaseUser: FirebaseUser

    private val testEmail = "test@example.com"
    private val testPassword = "password"
    private val testName = "Test User"

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        
        // Make network always available for tests
        every { mockNetworkUtils.isNetworkAvailable(any()) } returns true
        // Set the mock implementation
        NetworkUtils.setImplementation(mockNetworkUtils)
        
        // Setup the mock before creating the ViewModel
        // Use coEvery for suspend functions
        coEvery { mockAuthRepository.getCurrentUser() } returns null
        
        // Initialize the ViewModel
        authViewModel = AuthViewModel(mockAuthRepository, mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // Restore default implementation
        NetworkUtils.setImplementation(edu.utap.auth.utils.NetworkUtilsImpl())
    }

    @Test
    fun `initial state is Idle`() = runTest {
        // Create a new ViewModel instance to test initial state
        val newViewModel = AuthViewModel(mockAuthRepository, mockContext)
        assertEquals(AuthState.Idle, newViewModel.authState.value)
        
        // Then advance the test dispatcher to allow coroutines to complete
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Now check the state after initialization completes
        assertEquals(AuthState.Unauthenticated, newViewModel.authState.value)
    }

    @Test
    fun `login success sets Authenticated state`() = runTest {
        coEvery { mockAuthRepository.loginUser(testEmail, testPassword) } returns Result.success(mockFirebaseUser)

        authViewModel.login(testEmail, testPassword)
        assertEquals(AuthState.Loading, authViewModel.authState.value)

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Authenticated)
    }

    @Test
    fun `login failure sets Error state`() = runTest {
        val errorMessage = "Invalid credentials"
        coEvery { mockAuthRepository.loginUser(testEmail, testPassword) } returns Result.failure(Exception(errorMessage))

        authViewModel.login(testEmail, testPassword)
        assertEquals(AuthState.Loading, authViewModel.authState.value)

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Error)
        assertEquals(errorMessage, (finalState as AuthState.Error).message)
    }

    @Test
    fun `register success sets Authenticated state`() = runTest {
        coEvery { mockAuthRepository.registerUser(testEmail, testPassword, testName) } returns Result.success(mockFirebaseUser)

        authViewModel.register(testEmail, testPassword, testName)
        assertEquals(AuthState.Loading, authViewModel.authState.value)

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Authenticated)
    }

    @Test
    fun `register failure sets Error state`() = runTest {
        val errorMessage = "Registration failed"
        coEvery { mockAuthRepository.registerUser(testEmail, testPassword, testName) } returns Result.failure(Exception(errorMessage))

        authViewModel.register(testEmail, testPassword, testName)
        assertEquals(AuthState.Loading, authViewModel.authState.value)

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Error)
        assertEquals(errorMessage, (finalState as AuthState.Error).message)
    }

    @Test
    fun `logout sets Unauthenticated state`() = runTest {
        coJustRun { mockAuthRepository.logout() }
        
        authViewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.Unauthenticated, authViewModel.authState.value)
    }

    @Test
    fun `resetPassword success sets PasswordResetSent state`() = runTest {
        coEvery { mockAuthRepository.resetPassword(testEmail) } returns Result.success(Unit)

        authViewModel.resetPassword(testEmail)
        assertEquals(AuthState.Loading, authViewModel.authState.value)

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.PasswordResetSent)
    }

    @Test
    fun `resetPassword failure sets Error state`() = runTest {
        val errorMessage = "Password reset failed"
        coEvery { mockAuthRepository.resetPassword(testEmail) } returns Result.failure(Exception(errorMessage))

        authViewModel.resetPassword(testEmail)
        assertEquals(AuthState.Loading, authViewModel.authState.value)

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Error)
        assertEquals(errorMessage, (finalState as AuthState.Error).message)
    }
    
    @Test
    fun `resetPassword with no network sets Error state`() = runTest {
        // Set network as unavailable
        every { mockNetworkUtils.isNetworkAvailable(any()) } returns false
        
        authViewModel.resetPassword(testEmail)
        
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Error)
        assertTrue((finalState as AuthState.Error).message.contains("internet connection"))
    }
}