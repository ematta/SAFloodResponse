package edu.utap.auth.utils

import android.content.Context

/**
 * A provider for NetworkUtilsInterface that uses the application context
 * This helps avoid memory leaks by ensuring we don't hold references to Activity contexts
 */
class NetworkUtilsProvider {
    companion object {
        /**
         * Get a NetworkUtilsInterface instance that uses the application context
         */
        fun getNetworkUtils(): NetworkUtilsInterface {
            return object : NetworkUtilsInterface {
                override fun isNetworkAvailable(context: Context): Boolean {
                    // Ignore the passed context and use NetworkUtils with application context
                    return NetworkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())
                }
            }
        }
    }
}