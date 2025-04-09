package edu.utap.auth

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import edu.utap.models.AuthViewModel
import edu.utap.models.FirestoreUser
import edu.utap.repository.AuthRepositoryImpl
import edu.utap.repository.AuthRepositoryInterface
import edu.utap.utils.NetworkUtils
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var networkUtils: NetworkUtils
    private lateinit var context: Context
    private lateinit var firebaseUser: FirebaseUser

    private val testUser: FirestoreUser = FirestoreUser(
        userId = "uid123",
        email = "test@example.com",
        name = "Test User",
    )

    private val currentUserFlow = MutableStateFlow<FirestoreUser?>(null)
    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.Idle.Unauthenticated)

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        authRepository = mockk(relaxed = true)
        networkUtils = mockk(relaxed = true)
        context = mockk(relaxed = true)
        firebaseUser = mockk(relaxed = true)

        mockkConstructor(AuthStateManager::class)
        every { anyConstructed<AuthStateManager>().authState } returns authStateFlow
        every { anyConstructed<AuthStateManager>().currentUser } returns currentUserFlow
        every { anyConstructed<AuthStateManager>().updateState(any()) } answers {
            authStateFlow.value = firstArg()
        }
        every { anyConstructed<AuthStateManager>().updateCurrentUser(any()) } answers {
            currentUserFlow.value = firstArg()
        }
        every { anyConstructed<AuthStateManager>().resetState() } answers {
            authStateFlow.value = AuthState.Idle.Unauthenticated
            currentUserFlow.value = null
        }

        viewModel = AuthViewModel(authRepository, networkUtils, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `checkAuthState sets authenticated when user exists`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns firebaseUser
        coEvery { authRepository.getUserById(testUser.userId) } returns Result.success(testUser)
        coEvery { authRepository.updateUser(any()) } returns Result.success(testUser)

        viewModel.checkAuthState()
        advanceUntilIdle()

        assertEquals(AuthState.Idle.Authenticated, authStateFlow.value)
        assertEquals(testUser, currentUserFlow.value)
    }

    @Test
    fun `checkAuthState restores cached user if no firebase user but cache valid`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns null

        mockkObject(edu.utap.utils.DefaultSecurePrefsProvider)
        every { anyConstructed<edu.utap.utils.DefaultSecurePrefsProvider>().getCachedUserData() } returns
            """{"userId":"uid123","email":"test@example.com","name":"Test User","role":"user"}"""
        every { anyConstructed<edu.utap.utils.DefaultSecurePrefsProvider>().getInitialLoginTime() } returns System.currentTimeMillis()

        viewModel.checkAuthState()
        advanceUntilIdle()

        assertEquals(AuthState.Idle.Authenticated, authStateFlow.value)
        assertEquals("uid123", currentUserFlow.value?.userId)

        unmockkObject(edu.utap.utils.DefaultSecurePrefsProvider)
    }

    @Test
    fun `checkAuthState resets state if no user and cache expired`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns null

        mockkObject(edu.utap.utils.DefaultSecurePrefsProvider)
        every { anyConstructed<edu.utap.utils.DefaultSecurePrefsProvider>().getCachedUserData() } returns null
        every { anyConstructed<edu.utap.utils.DefaultSecurePrefsProvider>().getInitialLoginTime() } returns 0L

        viewModel.checkAuthState()
        advanceUntilIdle()

        assertEquals(AuthState.Idle.Unauthenticated, authStateFlow.value)
        assertNull(currentUserFlow.value)

        unmockkObject(edu.utap.utils.DefaultSecurePrefsProvider)
    }

    @Test
    fun `register success updates user and calls callback`() = runTest {
        val callback = mockk<(Boolean, String?) -> Unit>(relaxed = true)
        coEvery { authRepository.getUserByEmail(any()) } returns Result.success(testUser)
        coEvery { authRepository.updateUser(any()) } returns Result.success(testUser)

        viewModel.register("test@example.com", "password", "Test User", "user", callback)
        advanceUntilIdle()

        verify { callback(true, null) }
        assertEquals(AuthState.Idle.Authenticated, authStateFlow.value)
        assertEquals(testUser, currentUserFlow.value)
    }

    @Test
    fun `register failure calls callback with error`() = runTest {
        val callback = mockk<(Boolean, String?) -> Unit>(relaxed = true)
        coEvery { authRepository.getUserByEmail(any()) } returns Result.failure(Exception("error"))

        viewModel.register("test@example.com", "password", "Test User", "user", callback)
        advanceUntilIdle()

        verify { callback(false, "error") }
    }

    @Test
    fun `login success updates user and calls callback`() = runTest {
        val callback = mockk<(Boolean, String?) -> Unit>(relaxed = true)
        coEvery { authRepository.getUserByEmail(any()) } returns Result.success(testUser)

        viewModel.login("test@example.com", "password", callback)
        advanceUntilIdle()

        verify { callback(true, null) }
        assertEquals(AuthState.Idle.Authenticated, authStateFlow.value)
        assertEquals(testUser, currentUserFlow.value)
    }

    @Test
    fun `logout resets state`() = runTest {
        coEvery { authRepository.logout() } just Runs

        val result = viewModel.logout()
        advanceUntilIdle()

        assertEquals(AuthState.Idle.Unauthenticated, result)
        assertEquals(AuthState.Idle.Unauthenticated, authStateFlow.value)
        assertNull(currentUserFlow.value)
    }

    @Test
    fun `resetPassword updates state and calls callback`() = runTest {
        val callback = mockk<(Boolean, String?) -> Unit>(relaxed = true)

        viewModel.resetPassword("test@example.com", callback)
        advanceUntilIdle()

        verify { callback(true, null) }
        assertEquals(AuthState.Idle.PasswordResetSent, authStateFlow.value)
    }

    @Test
    fun `updateUserRole updates role if different`() = runTest {
        coEvery { authRepository.getUserById("uid123") } returns Result.success(testUser)
        coEvery { authRepository.updateUser(any()) } returns Result.success(testUser.copy(role = "admin"))

        val result = viewModel.updateUserRole("uid123", "admin")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `updateUserRole returns success if role same`() = runTest {
        coEvery { authRepository.getUserById("uid123") } returns Result.success(testUser)

        val result = viewModel.updateUserRole("uid123", "user")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `hasPermission returns false if no user`() {
        currentUserFlow.value = null
        assertFalse(viewModel.hasPermission("user"))
    }

    @Test
    fun `hasPermission returns true if user role sufficient`() {
        currentUserFlow.value = testUser.copy(role = "admin")
        assertTrue(viewModel.hasPermission("moderator"))
    }

    @Test
    fun `hasPermission returns false if user role insufficient`() {
        currentUserFlow.value = testUser.copy(role = "user")
        assertFalse(viewModel.hasPermission("admin"))
    }
}
