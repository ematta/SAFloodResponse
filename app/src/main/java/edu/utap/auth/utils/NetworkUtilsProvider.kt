package edu.utap.auth.utils

import android.content.Context

/**
 * Provider for network utility functions.
 * 
 * This singleton object provides access to the NetworkUtilsInterface implementation.
 * It follows the service locator pattern, allowing the actual implementation
 * to be swapped out, particularly useful for testing.
 */
object NetworkUtilsProvider {
    // Default implementation of network utilities
    private var networkUtils: NetworkUtilsInterface = NetworkUtilsImpl()
    
    /**
     * Gets the current network utilities implementation.
     * 
     * @return The current NetworkUtilsInterface implementation
     */
    fun getNetworkUtils(): NetworkUtilsInterface {
        return networkUtils
    }
    
    /**
     * Sets a custom network utilities implementation.
     * 
     * This method is primarily used for testing to inject mock implementations.
     * 
     * @param utils The NetworkUtilsInterface implementation to use
     */
    // For testing purposes only
    fun setNetworkUtils(utils: NetworkUtilsInterface) {
        networkUtils = utils
    }
}