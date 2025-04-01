package edu.utap.auth

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseUser
import edu.utap.auth.model.AuthViewModel
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.auth.model.FirestoreUser
import edu.utap.utils.NetworkUtils
import edu.utap.utils.NetworkUtilsInterface
import edu.utap.utils.ApplicationContextProvider
// Removed import edu.utap.db.UserEntity as FirebaseUser is already imported
import edu.utap.utils.NetworkUtilsImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val role = "regular"

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
        every { mockFirebaseUser.uid } returns "test-uid"
        every { mockFirebaseUser.email } returns testEmail
        every { mockFirebaseUser.displayName } returns testName
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
        
        // Removed mocking for getLocalUserById returning UserEntity
        // coEvery { mockAuthRepository.getLocalUserById(any()) } returns Result.success(...) // Needs update based on refactored repository
        
        authViewModel = AuthViewModel(mockAuthRepository, mockNetworkUtils)
        testDispatcher.scheduler.advanceUntilIdle() // Process initial coroutine
    }

    @After
    fun tearDown() {
        clearAllMocks()
        FirebaseApp.clearInstancesForTest()
        Dispatchers.resetMain()
        // Restore default implementations
        NetworkUtils.setImplementation(NetworkUtilsImpl())
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
        // Verify initial state after initialization
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.Idle.Unauthenticated, authViewModel.authState.value)

        coEvery { mockAuthRepository.loginUser(testEmail, testPassword) } returns Result.success(mockFirebaseUser)
        coEvery { mockAuthRepository.getUserById(any()) } returns Result.success(FirestoreUser(
            userId = "test-uid",
            email = testEmail,
            name = testName,
            role = role
        ))
        
        authViewModel.login(testEmail, testPassword) { _, _ -> }
        
        // Wait for all coroutines to complete
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify initial auth state
        val initialState = authViewModel.authState.value
        assertTrue(initialState is AuthState.Idle.Authenticated)
        assertEquals(testEmail, (initialState as AuthState.Idle.Authenticated).user.email)

        // Trigger login and verify loading state immediately
        authViewModel.login(testEmail, testPassword) { _, _ -> }
        assertEquals(AuthState.Loading.Login, authViewModel.authState.value)

        // Advance time and verify final auth state
        testDispatcher.scheduler.advanceUntilIdle()
        val endState = authViewModel.authState.value
        assertTrue("Expected Authenticated state but got $endState",
            endState is AuthState.Idle.Authenticated)
    }

    @Test
    fun `login failure sets Error state`() = runTest {
        val errorMessage = "Invalid credentials"
        coEvery { mockAuthRepository.loginUser(testEmail, testPassword) } returns Result.failure(Exception(errorMessage))

        authViewModel.login(testEmail, testPassword) { success, message ->
            if (success) {
                throw Exception("Login succeeded unexpectedly")
            }
        }
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutine completion

        val finalState = authViewModel.authState.value

        assertTrue(finalState is AuthState.Error)
        assertEquals(errorMessage, (finalState as AuthState.Error.Authentication).message)
    }

    @Test
    fun `register success sets Authenticated state`() = runTest {
        coEvery { mockAuthRepository.registerUser(testEmail, testPassword, testName, role) } returns Result.success(mockFirebaseUser)
        coEvery { mockAuthRepository.getUserById(any()) } returns Result.success(FirestoreUser(
            userId = "test-uid",
            email = testEmail,
            name = testName,
            role = role
        ))

        authViewModel.register(testEmail, testPassword, testName)
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Idle.Authenticated)
    }

    @Test
    fun `register failure sets Error state`() = runTest {
        val errorMessage = AuthState.Error.Validation("Registration failed")
        coEvery { mockAuthRepository.registerUser(testEmail, testPassword, testName, role) } returns Result.failure(Exception(errorMessage.message))

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

        authViewModel.resetPassword(testEmail) { success, message -> }
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Idle.PasswordResetSent)
    }

    @Test
    fun `resetPassword failure sets Error state`() = runTest {
        val errorMessage = AuthState.Error.Authentication("Password reset failed")
        coEvery { mockAuthRepository.resetPassword(testEmail) } returns Result.failure(Exception(errorMessage.message))

        authViewModel.resetPassword(testEmail) { success, message -> }
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Error)
        assertEquals(errorMessage, (finalState as AuthState.Error))
    }

    @Test
    fun `resetPassword with no network sets Error state`() = runTest {
        every { mockNetworkUtils.isNetworkAvailable(any()) } returns false
        
        authViewModel.resetPassword(testEmail) { success, message -> }
        testDispatcher.scheduler.advanceUntilIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = authViewModel.authState.value
        assertTrue(finalState is AuthState.Error)
        val errorMessage = (finalState as AuthState.Error.Network)
        assertTrue(errorMessage.message.contains("internet connection") || errorMessage.message.contains("network"))
    }
}
