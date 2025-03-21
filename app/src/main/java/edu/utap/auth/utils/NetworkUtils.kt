package edu.utap.auth.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Interface for network utilities that can be mocked in tests
 */
interface NetworkUtilsInterface {
    fun isNetworkAvailable(context: Context): Boolean
}

/**
 * Implementation of NetworkUtilsInterface for production use
 */
class NetworkUtilsImpl : NetworkUtilsInterface {
    override fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/**
 * Object that delegates to implementation - can be replaced in tests
 */
object NetworkUtils {
    private var implementation: NetworkUtilsInterface = NetworkUtilsImpl()
    
    // For testing purposes only
    fun setImplementation(impl: NetworkUtilsInterface) {
        implementation = impl
    }
    
    fun isNetworkAvailable(context: Context): Boolean {
        return implementation.isNetworkAvailable(context)
    }
}