package edu.utap.auth
import edu.utap.ui.viewmodel.AuthViewModel
import edu.utap.models.FirestoreUser
import edu.utap.auth.MainDispatcherRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AuthFlowManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var authViewModel: AuthViewModel

    private lateinit var authFlowManager: AuthFlowManager

    private lateinit var authStateFlow: MutableStateFlow<AuthState>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        authStateFlow = MutableStateFlow(AuthState.Idle.Unauthenticated)
        coEvery { authViewModel.authState } returns authStateFlow

        coEvery { authViewModel.restoreAuthState() } returns Unit
        coEvery { authViewModel.getCachedUser() } returns null
        coEvery { authViewModel.isAuthExpired() } returns true
        coEvery { authViewModel.clearAuthCache() } returns Unit
        coEvery { authViewModel.login(any(), any(), any()) } returns Unit
        coEvery { authViewModel.register(any(), any(), any(), any(), any()) } returns Unit
        coEvery { authViewModel.logout() } returns AuthState.Idle.Unauthenticated
        coEvery { authViewModel.resetPassword(any(), any()) } returns Unit
    }

    @Test
    fun `init sets isAuthenticated true when cached user exists and not expired`() = runTest {
        val cachedUser = FirestoreUser()
        coEvery { authViewModel.getCachedUser() } returns cachedUser
        coEvery { authViewModel.isAuthExpired() } returns false

        val authenticatedFlow = MutableStateFlow<AuthState>(AuthState.Idle.Authenticated)
        coEvery { authViewModel.authState } returns authenticatedFlow

        authFlowManager = AuthFlowManager(authViewModel)

        assertTrue(authFlowManager.isAuthenticated.value)
    }

    @Test
    fun `init sets isAuthenticated false and clears cache when cached user is null`() = runTest {
        coEvery { authViewModel.getCachedUser() } returns null

        authFlowManager = AuthFlowManager(authViewModel)

        assertFalse(authFlowManager.isAuthenticated.value)
        coVerify { authViewModel.clearAuthCache() }
    }

    @Test
    fun `init sets isAuthenticated false and clears cache when cached user is expired`() = runTest {
        val cachedUser = FirestoreUser()
        coEvery { authViewModel.getCachedUser() } returns cachedUser
        coEvery { authViewModel.isAuthExpired() } returns true

        authFlowManager = AuthFlowManager(authViewModel)

        assertFalse(authFlowManager.isAuthenticated.value)
        coVerify { authViewModel.clearAuthCache() }
    }

    @Test
    fun `login delegates to AuthViewModel`() = runTest {
        authFlowManager = AuthFlowManager(authViewModel)

        authFlowManager.login("email", "password") { _, _ -> }

        coVerify { authViewModel.login("email", "password", any()) }
    }

    @Test
    fun `register delegates to AuthViewModel`() = runTest {
        authFlowManager = AuthFlowManager(authViewModel)

        authFlowManager.register("email", "password", "name", "role") { _, _ -> }

        coVerify { authViewModel.register("email", "password", "name", "role", any()) }
    }

    @Test
    fun `logout delegates to AuthViewModel`() = runTest {
        authFlowManager = AuthFlowManager(authViewModel)

        authFlowManager.logout()

        coVerify { authViewModel.logout() }
    }

    @Test
    fun `resetPassword delegates to AuthViewModel`() = runTest {
        authFlowManager = AuthFlowManager(authViewModel)

        authFlowManager.resetPassword("email") { _, _ -> }

        coVerify { authViewModel.resetPassword("email", any()) }
    }

    @Test
    fun `clearAuthCache delegates to AuthViewModel`() = runTest {
        authFlowManager = AuthFlowManager(authViewModel)

        authFlowManager.clearAuthCache()

        coVerify { authViewModel.clearAuthCache() }
    }

    @Test
    fun `isAuthenticated updates to true when authState emits Authenticated`() = runTest {
        authFlowManager = AuthFlowManager(authViewModel)

        authStateFlow.value = AuthState.Idle.Authenticated

        assertTrue(authFlowManager.isAuthenticated.value)
    }

    @Test
    fun `isAuthenticated updates to false when authState emits Unauthenticated`() = runTest {
        authFlowManager = AuthFlowManager(authViewModel)

        authStateFlow.value = AuthState.Idle.Unauthenticated

        assertFalse(authFlowManager.isAuthenticated.value)
    }

    @Test
    fun `isAuthenticated updates to false when authState emits Loading`() = runTest {
        authFlowManager = AuthFlowManager(authViewModel)

        authStateFlow.value = AuthState.Loading.InProgress

        assertFalse(authFlowManager.isAuthenticated.value)
    }

    @Test
    fun `isAuthenticated updates to false when authState emits Error`() = runTest {
        authFlowManager = AuthFlowManager(authViewModel)

        authStateFlow.value = AuthState.Error.Generic("error")

        assertFalse(authFlowManager.isAuthenticated.value)
    }
}
