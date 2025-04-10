package edu.utap.auth

import android.content.Context
import edu.utap.user.MainDispatcherRule
import edu.utap.utils.ApplicationContextProviderInterface
import edu.utap.utils.NetworkUtils
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class NetworkOperationHandlerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var contextProvider: ApplicationContextProviderInterface

    private lateinit var handler: NetworkOperationHandler

    private val noNetworkMessage = "No internet connection. Please check your network settings and try again."

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(NetworkUtils)
        every { contextProvider.getApplicationContext() } returns mockk(relaxed = true)
        handler = NetworkOperationHandler(NetworkUtils, contextProvider)
    }

    @Test
    fun `checkNetworkAvailability returns null when network is available`() = runTest {
        every { NetworkUtils.isNetworkAvailable(any()) } returns true

        val result = handler.checkNetworkAvailability()

        assertNull(result)
    }

    @Test
    fun `checkNetworkAvailability returns AuthState_Error_Network when network is unavailable`() = runTest {
        every { NetworkUtils.isNetworkAvailable(any()) } returns false

        val result = handler.checkNetworkAvailability()

        assertNotNull(result)
        assertTrue(result is AuthState.Error.Network)
        assertEquals(noNetworkMessage, (result as AuthState.Error.Network).message)
    }

    @Test
    fun `executeWithNetworkCheck returns failure when network is unavailable`() = runTest {
        every { NetworkUtils.isNetworkAvailable(any()) } returns false

        val result = handler.executeWithNetworkCheck { "Should not run" }

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertEquals(noNetworkMessage, exception?.message)
    }

    @Test
    fun `executeWithNetworkCheck returns success when network is available and operation succeeds`() = runTest {
        every { NetworkUtils.isNetworkAvailable(any()) } returns true

        val expected = "Success"
        val result = handler.executeWithNetworkCheck { expected }

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }

    @Test
    fun `executeWithNetworkCheck returns failure when network is available but operation throws`() = runTest {
        every { NetworkUtils.isNetworkAvailable(any()) } returns true

        val exception = RuntimeException("Operation failed")
        val result = handler.executeWithNetworkCheck { throw exception }

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
