package edu.utap.auth

import edu.utap.models.FirestoreUser
import io.mockk.MockKAnnotations
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import edu.utap.auth.MainDispatcherRule

class AuthStateManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authStateManager: AuthStateManager

    private val dummyUser = FirestoreUser()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        authStateManager = AuthStateManager()
    }

    @Test
    fun `updateState updates authState and currentUser`() = runTest {
        val newState = AuthState.Idle.Authenticated
        authStateManager.updateState(newState, dummyUser)

        assertEquals(newState, authStateManager.authState.value)
        assertEquals(dummyUser, authStateManager.currentUser.value)
    }

    @Test
    fun `updateCurrentUser updates only currentUser`() = runTest {
        authStateManager.updateCurrentUser(dummyUser)

        assertEquals(dummyUser, authStateManager.currentUser.value)
    }

    @Test
    fun `resetState resets authState and clears currentUser`() = runTest {
        authStateManager.updateState(AuthState.Idle.Authenticated, dummyUser)

        authStateManager.resetState()

        assertEquals(AuthState.Idle.Unauthenticated, authStateManager.authState.value)
        assertEquals(null, authStateManager.currentUser.value)
    }
}
