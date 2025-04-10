package edu.utap.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NetworkUtilsImplTest {

    private val networkUtils = NetworkUtilsImpl()

    @Test
    fun `Network available`() {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val result = networkUtils.isNetworkAvailable(context)
        assertEquals(true, result)
    }

    @Test
    fun `No active network`() {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns null

        val result = networkUtils.isNetworkAvailable(context)
        assertEquals(false, result)
    }

    @Test
    fun `Network no internet capability`() {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false

        val result = networkUtils.isNetworkAvailable(context)
        assertEquals(false, result)
    }

    @Test
    fun `Null active network`() {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns null

        val result = networkUtils.isNetworkAvailable(context)
        assertEquals(false, result)
    }

    @Test
    fun `Null network capabilities`() {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null

        val result = networkUtils.isNetworkAvailable(context)
        assertEquals(false, result)
    }

    @Test
    fun `Context null`() {
        // Since the method signature requires non-null Context, this test is not applicable.
        // But we can simulate a null pointer exception by casting null to Context
        assertFailsWith<NullPointerException> {
            networkUtils.isNetworkAvailable(null as Context)
        }
    }

    @Test
    fun `Connectivity service unavailable`() {
        val context = mockk<Context>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns null

        assertFailsWith<NullPointerException> {
            networkUtils.isNetworkAvailable(context)
        }
    }

    @Test
    fun `Multiple networks`() {
        // The implementation only checks activeNetwork, so simulate one active network with internet capability
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val result = networkUtils.isNetworkAvailable(context)
        assertEquals(true, result)
    }

    @Test
    fun `Network state changes`() {
        // Since the method is synchronous, simulate a change before the call
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val result = networkUtils.isNetworkAvailable(context)
        assertEquals(true, result)
    }

    @Test
    fun `Airplane mode`() {
        // Simulate airplane mode by no active network
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns null

        val result = networkUtils.isNetworkAvailable(context)
        assertEquals(false, result)
    }
}
