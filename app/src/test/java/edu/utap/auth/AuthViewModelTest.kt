package edu.utap.auth

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseUser
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.auth.utils.NetworkUtils
import edu.utap.auth.utils.NetworkUtilsInterface
import edu.utap.auth.utils.ApplicationContextProvider
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
    
    @MockK(relaxed = true)
    private lateinit var mockAuthRepository: AuthRepositoryInterface
    
    @MockK
    private lateinit var mockContext: Context
    
    @MockK(relaxed = true)
    private lateinit var mockNetworkUtils: NetworkUtilsInterface
    
    private lateinit var authViewModel: AuthViewModel
    
    @MockK(relaxed = true)
    private lateinit var mockFirebaseUser: FirebaseUser

    private val testEmail = "test@example.com"
    private val testPassword = "password"
    private val testName = "Test User"

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        
        // Mock application context provider
        mockkObject(ApplicationContextProvider)
        every { ApplicationContextProvider.getApplicationContext() } returns mockContext

        // Avoid real Firebase options
        mockkConstructor(FirebaseOptions.Builder::class)
        every { anyConstructed<FirebaseOptions.Builder>().setApplicationId(any()) } returns mockk()
        every { anyConstructed<FirebaseOptions.Builder>().setApiKey(any()) } returns mockk()
        every { anyConstructed<FirebaseOptions.Builder>().build() } returns mockk()

        // Mock SystemClock for Firebase initialization
        mockkStatic(android.os.SystemClock::class, android.os.Process::class)
        every { android.os.SystemClock.elapsedRealtime() } returns 0L
        every { android.os.Process.myPid() } returns 1234

        // Initialize Firebase with test context
        mockkStatic(FirebaseApp::class)
        val mockFirebaseApp = mockk<FirebaseApp>(relaxed = true)
        every { FirebaseApp.initializeApp(any()) } returns mockFirebaseApp
        every { FirebaseApp.getInstance() } returns mockFirebaseApp
        
        // Reset network mock to default available state
        clearMocks(mockNetworkUtils)
        every { mockNetworkUtils.isNetworkAvailable(any()) } returns true
        NetworkUtils.setImplementation(mockNetworkUtils)
        
        coEvery { mockAuthRepository.getCurrentUser() } returns null
        
        // Mock UserEntity for getLocalUserById calls
        val mockUserEntity = mockk<edu.utap.auth.db.UserEntity>(relaxed = true)
        coEvery { mockAuthRepository.getLocalUserById(any()) } returns Result.success(mockUserEntity)
        
        authViewModel = AuthViewModel(mockAuthRepository, mockNetworkUtils)
        testDispatcher.scheduler.advanceUntilIdle() // Process initial coroutine
    }

    @After
    fun tearDown() {
        clearAllMocks()
        FirebaseApp.clearInstancesForTest()
        Dispatchers.resetMain()
        // Restore default implementations
        NetworkUtils.setImplementation(edu.utap.auth.utils.NetworkUtilsImpl())
        unmockkObject(ApplicationContextProvider)
    }

    @Test
    fun `initial state is Idle`() = runTest {
        val newViewModel = AuthViewModel(mockAuthRepository, mockNetworkUtils)
        // Immediate state after creation (before coroutines run)
        assertEquals(AuthState.Idle.Initial, newViewModel.authState.value)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(AuthState.Idle.Unauthenticated, newViewModel.authState.value)
    }

    @Test
    fun `login success sets Authenticated state`() = runTest {
        coEvery { mockAuthRepository.loginUser(testEmail, testPassword) } returns Result.success(mockFirebaseUser)

        authViewModel.login(testEmail, testPassword)
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutine completion
        
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Idle.Authenticated)
    }

    @Test
    fun `login failure sets Error state`() = runTest {
        val errorMessage = AuthState.Error.Authentication("Invalid credentials")
        coEvery { mockAuthRepository.loginUser(testEmail, testPassword) } returns Result.failure(Exception(errorMessage.message))

        authViewModel.login(testEmail, testPassword)
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutine completion
        
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Error)
        assertEquals(errorMessage, (finalState as AuthState.Error))
    }

    @Test
    fun `register success sets Authenticated state`() = runTest {
        coEvery { mockAuthRepository.registerUser(testEmail, testPassword, testName) } returns Result.success(mockFirebaseUser)

        authViewModel.register(testEmail, testPassword, testName)
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Idle.Authenticated)
    }

    @Test
    fun `register failure sets Error state`() = runTest {
        val errorMessage = AuthState.Error.Validation("Registration failed")
        coEvery { mockAuthRepository.registerUser(testEmail, testPassword, testName) } returns Result.failure(Exception(errorMessage.message))

        authViewModel.register(testEmail, testPassword, testName)
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Error)
        assertEquals(errorMessage, (finalState as AuthState.Error))
    }

    @Test
    fun `logout sets Unauthenticated state`() = runTest {
        coJustRun { mockAuthRepository.logout() }
        
        authViewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.Idle.Unauthenticated, authViewModel.authState.value)
    }

    @Test
    fun `resetPassword success sets PasswordResetSent state`() = runTest {
        coEvery { mockAuthRepository.resetPassword(testEmail) } returns Result.success(Unit)

        authViewModel.resetPassword(testEmail)
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Idle.PasswordResetSent)
    }

    @Test
    fun `resetPassword failure sets Error state`() = runTest {
        val errorMessage = AuthState.Error.Authentication("Password reset failed")
        coEvery { mockAuthRepository.resetPassword(testEmail) } returns Result.failure(Exception(errorMessage.message))

        authViewModel.resetPassword(testEmail)
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Error)
        assertEquals(errorMessage, (finalState as AuthState.Error))
    }
    
    @Test
    fun `resetPassword with no network sets Error state`() = runTest {
        every { mockNetworkUtils.isNetworkAvailable(any()) } returns false
        
        authViewModel.resetPassword(testEmail)
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Error)
        val errorMessage = (finalState as AuthState.Error.Network)
        assertTrue(errorMessage.message.contains("internet connection") || errorMessage.message.contains("network"))
    }
}