package edu.utap.auth.utils

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
class NetworkMonitor(private val context: Context) {
    
    /**
     * Returns a Flow of NetworkState that emits the current network state and subsequent changes
     */
    fun networkState(): Flow<NetworkState> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            trySend(NetworkState.UNAVAILABLE)
            return@callbackFlow
        }
        
        // Check initial state
        val isConnected = NetworkUtils.isNetworkAvailable(context)
        trySend(if (isConnected) NetworkState.AVAILABLE else NetworkState.UNAVAILABLE)
        
        // Create network callback
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
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
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Unregister when the flow is cancelled
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}