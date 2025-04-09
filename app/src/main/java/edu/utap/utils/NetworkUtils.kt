package edu.utap.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Interface for network utility functions.
 *
 * This interface allows for dependency injection and easier testing
 * of components that need to check network connectivity.
 */
interface NetworkUtilsInterface {
    /**
     * Checks if the device has an active network connection.
     *
     * @param context Android context used to access system services
     * @return true if a network connection is available, false otherwise
     */
    fun isNetworkAvailable(context: Context): Boolean
}

/**
 * Implementation of NetworkUtilsInterface for production use
 */
class NetworkUtilsImpl : NetworkUtilsInterface {
    /**
     * Checks if the device has an active network connection.
     *
     * This method uses the ConnectivityManager to check for active network
     * capabilities. It verifies if the network has internet capability.
     *
     * @param context Android context used to access the ConnectivityManager
     * @return true if a network connection is available, false otherwise
     */
    override fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
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

    //
    fun isNetworkAvailable(context: Context): Boolean = implementation.isNetworkAvailable(context)
}
