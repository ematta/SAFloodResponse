package edu.utap.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Network connectivity state
 */
enum class NetworkState {
    AVAILABLE,
    UNAVAILABLE,
    LOSING,
    LOST
}

/**
 * A utility class that monitors network connectivity changes and provides a Flow of NetworkState
 */
class NetworkMonitor(
    private val context: Context,
    private val networkUtils: NetworkUtilsInterface
) {
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var connectivityManager: ConnectivityManager? = null


    /**
     * Represents the current state of the network connection.
     */
    fun networkState(): Flow<NetworkState> = callbackFlow {
        connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as? ConnectivityManager
        if (connectivityManager == null) {
            trySend(NetworkState.UNAVAILABLE)
            return@callbackFlow
        }

        // Check initial state
        val isConnected = networkUtils.isNetworkAvailable(context)
        trySend(if (isConnected) NetworkState.AVAILABLE else NetworkState.UNAVAILABLE)

        // Create network callback
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkState.AVAILABLE)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                trySend(NetworkState.LOSING)
            }

            override fun onLost(network: Network) {
                trySend(NetworkState.LOST)
            }

            override fun onUnavailable() {
                trySend(NetworkState.UNAVAILABLE)
            }
        }

        // Register callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)

        // Unregister when the flow is cancelled
        awaitClose {
            shutdown()
        }
    }

    /**
     * Clean up network monitoring resources
     */
    fun shutdown() {
        networkCallback?.let { callback ->
            connectivityManager?.unregisterNetworkCallback(callback)
            networkCallback = null
        }
        connectivityManager = null
    }
}
