package edu.utap.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.firebase.auth.FirebaseUser
import edu.utap.user.UserProfile
import edu.utap.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AuthViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var authRepository: AuthRepository
    
    @Mock
    private lateinit var userRepository: UserRepository
    
    private lateinit var authViewModel: AuthViewModel
    
    @Before
    fun setup() {
        // Initialize mocks outside of runTest to ensure proper initialization
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Initialize authViewModel with mocked repositories
        `when`(authRepository.getCurrentUser()).thenReturn(null)
        
        // Use runTest for mocking suspend functions
        runTest {
            // Mock userRepository.createUserProfile
            `when`(userRepository.createUserProfile(any())).thenReturn(Result.success(UserProfile()))
            authViewModel = AuthViewModel(authRepository, userRepository)
        }
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is Idle`() = runTest {
        // Given
        `when`(authRepository.getCurrentUser()).thenReturn(null)
        
        // When
        authViewModel = AuthViewModel(authRepository, userRepository)
        
        // Then
        // No need to assert initial state
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.Unauthenticated, authViewModel.authState.first())
    }
    
    @Test
    fun `initial state is Authenticated when user is logged in`() = runTest {
        // Given
        val mockUser = mock(FirebaseUser::class.java)
        `when`(authRepository.getCurrentUser()).thenReturn(mockUser)
        
        // When
        authViewModel = AuthViewModel(authRepository, userRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val authState = authViewModel.authState.first()
        assertTrue(authState is AuthState.Authenticated)
        assertEquals(mockUser, (authState as AuthState.Authenticated).user)
    }
    
    @Test
    fun `register updates state to Loading then Authenticated on success`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("test-uid")
        `when`(mockUser.email).thenReturn(email)
        `when`(mockUser.displayName).thenReturn(null)
        
        `when`(authRepository.getCurrentUser()).thenReturn(null)
        `when`(authRepository.registerUser(email, password)).thenReturn(Result.success(mockUser))
        
        // Initialize viewModel
        authViewModel = AuthViewModel(authRepository, userRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        authViewModel.register(email, password)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val authState = authViewModel.authState.first()
        assertTrue(authState is AuthState.Authenticated)
        assertEquals(mockUser, (authState as AuthState.Authenticated).user)
        verify(authRepository).registerUser(email, password)
        verify(userRepository).createUserProfile(any())
    }
    
    @Test
    fun `register updates state to Loading then Error on failure`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val errorMessage = "Registration failed"
        `when`(authRepository.getCurrentUser()).thenReturn(null)
        `when`(authRepository.registerUser(email, password)).thenReturn(Result.failure(Exception(errorMessage)))
        
        // Initialize viewModel
        authViewModel = AuthViewModel(authRepository, userRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        authViewModel.register(email, password)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val authState = authViewModel.authState.first()
        assertTrue(authState is AuthState.Error)
        assertEquals(errorMessage, (authState as AuthState.Error).message)
        verify(authRepository).registerUser(email, password)
    }
    
    @Test
    fun `login updates state to Loading then Authenticated on success`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val mockUser = mock(FirebaseUser::class.java)
        `when`(authRepository.getCurrentUser()).thenReturn(null)
        `when`(authRepository.loginUser(email, password)).thenReturn(Result.success(mockUser))
        
        // Initialize viewModel
        authViewModel = AuthViewModel(authRepository, userRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val authState = authViewModel.authState.first()
        assertTrue(authState is AuthState.Authenticated)
        assertEquals(mockUser, (authState as AuthState.Authenticated).user)
        verify(authRepository).loginUser(email, password)
    }
    
    @Test
    fun `login updates state to Loading then Error on failure`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val errorMessage = "Login failed"
        `when`(authRepository.getCurrentUser()).thenReturn(null)
        `when`(authRepository.loginUser(email, password)).thenReturn(Result.failure(Exception(errorMessage)))
        
        // Initialize viewModel
        authViewModel = AuthViewModel(authRepository, userRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        authViewModel.login(email, password)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val authState = authViewModel.authState.first()
        assertTrue(authState is AuthState.Error)
        assertEquals(errorMessage, (authState as AuthState.Error).message)
        verify(authRepository).loginUser(email, password)
    }
    
    @Test
    fun `logout updates state to Unauthenticated`() = runTest {
        // Given
        val mockUser = mock(FirebaseUser::class.java)
        `when`(authRepository.getCurrentUser()).thenReturn(mockUser)
        
        // Initialize viewModel
        authViewModel = AuthViewModel(authRepository, userRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(authViewModel.authState.first() is AuthState.Authenticated)
        
        // When
        authViewModel.logout()
        
        // Then
        assertEquals(AuthState.Unauthenticated, authViewModel.authState.first())
        verify(authRepository).logout()
    }
}