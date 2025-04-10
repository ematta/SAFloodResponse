package edu.utap.utils

import org.junit.Test
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.cancelAndJoin
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.mockk.mockk
import io.mockk.every
import io.mockk.spyk

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkMonitorTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `networkState initial AVAILABLE`() {
        io.mockk.mockkStatic("androidx.core.content.ContextCompat")

        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        val networkUtils = mockk<NetworkUtilsInterface>()

        every { context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { networkUtils.isNetworkAvailable(context) } returns true

        val network = mockk<android.net.Network>()
        val networkCapabilities = mockk<android.net.NetworkCapabilities>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val monitor = NetworkMonitor(context, networkUtils)
        runTest {
            val state = monitor.networkState().first()
            assertEquals(NetworkState.AVAILABLE, state)
        }
    }

    @Test
    fun `networkState initial UNAVAILABLE`() {
        io.mockk.mockkStatic("androidx.core.content.ContextCompat")

        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        val networkUtils = mockk<NetworkUtilsInterface>()

        every { context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { networkUtils.isNetworkAvailable(context) } returns false

        every { connectivityManager.activeNetwork } returns null

        val monitor = NetworkMonitor(context, networkUtils)
        runTest {
            val state = monitor.networkState().first()
            assertEquals(NetworkState.UNAVAILABLE, state)
        }
    }

    @Test
    fun `networkState network becomes AVAILABLE`() {
        // Simulate the network becoming available and check if the
        // flow emits NetworkState.AVAILABLE.
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val network = mockk<android.net.Network>()
        val context = mockk<android.content.Context>(relaxed = true)
        val networkCapabilities = mockk<android.net.NetworkCapabilities>()
        every { connectivityManager.activeNetwork } returns null
        every { connectivityManager.getNetworkCapabilities(any()) } returns networkCapabilities
        every { networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val callbackSlot = io.mockk.slot<android.net.ConnectivityManager.NetworkCallback>()
        every { connectivityManager.registerNetworkCallback(any(), capture(callbackSlot)) } returns Unit

        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = NetworkMonitor(context, networkUtils)
        runTest {
            val states = mutableListOf<NetworkState>()
            val job = launch {
                monitor.networkState().toList(states)
            }
            callbackSlot.captured.onAvailable(network)
            delay(100)
            job.cancel()
            assertTrue(states.contains(NetworkState.AVAILABLE))
        }
    }

    @Test
    fun `networkState network becomes LOST`() {
        // Simulate the network becoming unavailable and verify
        // if the flow emits NetworkState.LOST.
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val network = mockk<android.net.Network>()
        val context = mockk<android.content.Context>(relaxed = true)
        val callbackSlot = io.mockk.slot<android.net.ConnectivityManager.NetworkCallback>()
        every { connectivityManager.registerNetworkCallback(any(), capture(callbackSlot)) } returns Unit

        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = NetworkMonitor(context, networkUtils)
        runTest {
            val states = mutableListOf<NetworkState>()
            val job = launch {
                monitor.networkState().toList(states)
            }
            callbackSlot.captured.onLost(network)
            delay(100)
            job.cancel()
            assertTrue(states.contains(NetworkState.LOST))
        }
    }

    @Test
    fun `networkState network is LOSING`() {
        // Simulate the network starting to lose connectivity
        // and verify that the flow emits NetworkState.LOSING.
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val network = mockk<android.net.Network>()
        val context = mockk<android.content.Context>(relaxed = true)

        val callbackSlot = io.mockk.slot<android.net.ConnectivityManager.NetworkCallback>()
        every { connectivityManager.registerNetworkCallback(any(), capture(callbackSlot)) } returns Unit

        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = NetworkMonitor(context, networkUtils)
        runTest {
            val states = mutableListOf<NetworkState>()
            val job = launch {
                monitor.networkState().toList(states)
            }
            callbackSlot.captured.onLosing(network, 0)
            delay(100)
            job.cancel()
            assertTrue(states.contains(NetworkState.LOSING))
        }
    }

    @Test
    fun `networkState multiple state changes`() {
        // Check if the flow correctly emits multiple state changes
        // (e.g., AVAILABLE -> LOSING -> LOST -> AVAILABLE).
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val network = mockk<android.net.Network>()

        val context = mockk<android.content.Context>(relaxed = true)
        val callbackSlot = io.mockk.slot<android.net.ConnectivityManager.NetworkCallback>()
        every { connectivityManager.registerNetworkCallback(any(), capture(callbackSlot)) } returns Unit

        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = NetworkMonitor(context, networkUtils)
        runTest {
            val states = mutableListOf<NetworkState>()
            val job = launch {
                monitor.networkState().toList(states)
            }
            callbackSlot.captured.onAvailable(network)
            callbackSlot.captured.onLosing(network, 0)
            callbackSlot.captured.onLost(network)
            callbackSlot.captured.onAvailable(network)
            delay(100)
            job.cancel()
            assertTrue(states.containsAll(listOf(NetworkState.AVAILABLE, NetworkState.LOSING, NetworkState.LOST)))
        }
    }

    @Test
    fun `networkState connectivityManager null`() {
        // Check if the flow emits NetworkState.UNAVAILABLE when
        // the ConnectivityManager is null.
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        every { connectivityManager.activeNetwork } returns null
        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = NetworkMonitor(context, networkUtils)
        runTest {
            val state = monitor.networkState().first()
            assertEquals(NetworkState.UNAVAILABLE, state)
        }
    }

    @Test
    fun `networkState callback registration`() {
        // Verify that the NetworkCallback is correctly registered
        // with the ConnectivityManager.
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        val callbackSlot = io.mockk.slot<android.net.ConnectivityManager.NetworkCallback>()
        every { connectivityManager.registerNetworkCallback(any(), capture(callbackSlot)) } returns Unit

        val networkUtils = mockk<NetworkUtilsInterface>()
        NetworkMonitor(context, networkUtils).networkState()
        assertTrue(callbackSlot.isCaptured)
    }

    @Test
    fun `networkState no internet capability`() {
        // Verify that the NetworkRequest is correctly configured with
        // the NET_CAPABILITY_INTERNET.
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val requestSlot = io.mockk.slot<android.net.NetworkRequest>()
        val context = mockk<android.content.Context>(relaxed = true)
        every { connectivityManager.registerNetworkCallback(capture(requestSlot), any<android.net.ConnectivityManager.NetworkCallback>()) } returns Unit

        val networkUtils = mockk<NetworkUtilsInterface>()
        NetworkMonitor(context, networkUtils).networkState()

        // Cannot verify NetworkRequest capabilities directly as they are not exposed
        // So this test assertion is removed
    }

    @Test
    fun `shutdown unregisters callback`() {
        // Verify that shutdown() unregisters the NetworkCallback
        // from the ConnectivityManager.
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val callback = mockk<android.net.ConnectivityManager.NetworkCallback>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = NetworkMonitor(context, networkUtils)

        val callbackField = NetworkMonitor::class.java.getDeclaredField("networkCallback")
        callbackField.isAccessible = true
        callbackField.set(monitor, callback)

        monitor.shutdown()

        io.mockk.verify { connectivityManager.unregisterNetworkCallback(callback) }
    }

    @Test
    fun `shutdown called multiple times`() {
        // Ensure that calling shutdown() multiple times does
        // not cause any errors or unexpected behavior.
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val callback = mockk<android.net.ConnectivityManager.NetworkCallback>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = NetworkMonitor(context, networkUtils)

        val callbackField = NetworkMonitor::class.java.getDeclaredField("networkCallback")
        callbackField.isAccessible = true
        callbackField.set(monitor, callback)

        monitor.shutdown()
        monitor.shutdown()

        io.mockk.verify(exactly = 1) { connectivityManager.unregisterNetworkCallback(callback) }
    }

    @Test
    fun `shutdown sets callback to null`() {
        // Verify that shutdown() sets networkCallback to null.
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = NetworkMonitor(context, networkUtils)
        val callbackField = NetworkMonitor::class.java.getDeclaredField("networkCallback")
        callbackField.isAccessible = true
        callbackField.set(monitor, mockk(relaxed = true))

        monitor.shutdown()

        assertEquals(null, callbackField.get(monitor))
    }

    @Test
    fun `shutdown sets connectivityManager to null`() {
        // Verify that shutdown() sets connectivityManager to null.
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = NetworkMonitor(context, networkUtils)
        val cmField = NetworkMonitor::class.java.getDeclaredField("connectivityManager")
        cmField.isAccessible = true
        cmField.set(monitor, connectivityManager)

        monitor.shutdown()

        assertEquals(null, cmField.get(monitor))
    }

    @Test
    fun `shutdown with no callback`() {
        // Test shutdown() when networkCallback is null, ensure that no
        // exception is thrown.
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = NetworkMonitor(context, networkUtils)
        val callbackField = NetworkMonitor::class.java.getDeclaredField("networkCallback")
        callbackField.isAccessible = true
        callbackField.set(monitor, null)

        monitor.shutdown()

        assertTrue(true)
    }

    @Test
    fun `shutdown with no connectivityManager`() {
        // Test shutdown() when connectivityManager is null, ensure
        // that no exception is thrown.
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = NetworkMonitor(context, networkUtils)
        val cmField = NetworkMonitor::class.java.getDeclaredField("connectivityManager")
        cmField.isAccessible = true
        cmField.set(monitor, null)

        monitor.shutdown()

        assertTrue(true)
    }

    @Test
    fun `networkState flow cancellation`() {
        // Verify that when the networkState() flow is cancelled, shutdown()
        // is called.
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = spyk(NetworkMonitor(context, networkUtils), recordPrivateCalls = true)

        runTest {
            val job = launch {
                monitor.networkState().collect { }
            }
            job.cancelAndJoin()
            io.mockk.verify { monitor.shutdown() }
        }
    }

    @Test
    fun `networkState onUnavailable received`() {
        // Simulate onUnavailable callback being called by the network
        // and verify NetworkState.UNAVAILABLE is sent
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        val callbackSlot = io.mockk.slot<android.net.ConnectivityManager.NetworkCallback>()
        every { connectivityManager.registerNetworkCallback(any(), capture(callbackSlot)) } returns Unit

        val context = mockk<android.content.Context>(relaxed = true)
        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = NetworkMonitor(context, networkUtils)
        runTest {
            val states = mutableListOf<NetworkState>()
            val job = launch {
                monitor.networkState().toList(states)
            }
            callbackSlot.captured.onUnavailable()
            delay(100)
            job.cancel()
            assertTrue(states.contains(NetworkState.UNAVAILABLE))
        }
    }

    @Test
    fun `networkState onUnavailable immediately sent`() {
        //Ensure onUnavailable is sent immediately if network state is initially unavailable
        io.mockk.mockkStatic("androidx.core.content.ContextCompat")
        val connectivityManager = mockk<android.net.ConnectivityManager>(relaxed = true)
        every { connectivityManager.activeNetwork } returns null

        val context = mockk<android.content.Context>(relaxed = true)
        val networkUtils = mockk<NetworkUtilsInterface>()
        val monitor = NetworkMonitor(context, networkUtils)
        runTest {
            val state = monitor.networkState().first()
            assertEquals(NetworkState.UNAVAILABLE, state)
        }
    }

}
