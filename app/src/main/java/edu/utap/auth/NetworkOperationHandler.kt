package edu.utap.auth

import android.util.Log
import edu.utap.utils.ApplicationContextProviderInterface
import edu.utap.utils.NetworkUtils

/**
 * Handles network-related operations for authentication.
 * This class provides methods to check network connectivity and handle network-dependent operations.
 */
class NetworkOperationHandler(
    private val networkUtils: NetworkUtils,
    private val context: ApplicationContextProviderInterface
) {
    /**
     * Checks if network is available and returns an error state if not.
     * @return AuthState.Error.Network if network is not available, null otherwise
     */
    fun checkNetworkAvailability(): AuthState.Error.Network? {
        val appContext = context.getApplicationContext()
        val isAvailable = networkUtils.isNetworkAvailable(appContext)
        Log.d(
            "NetworkOperationHandler_checkNetworkAvailability",
            "Network available: $isAvailable [Thread: ${Thread.currentThread().name}]"
        )

        return if (!isAvailable) {
            val error = AuthState.Error.Network(
                "No internet connection. Please check your network settings and try again."
            )
            Log.e(
                "NetworkOperationHandler_checkNetworkAvailability",
                "Network unavailable: ${error.message}"
            )
            error
        } else {
            null
        }
    }

    /**
     * Executes a network operation with network availability check.
     * @param operation The operation to execute
     * @return Result of the operation or network error state
     */
    suspend fun <T> executeWithNetworkCheck(operation: suspend () -> T): Result<T> {
        Log.d(
            "NetworkOperationHandler_executeWithNetworkCheck",
            "Starting network operation [Thread: ${Thread.currentThread().name}]"
        )

        val networkError = checkNetworkAvailability()
        return if (networkError != null) {
            Log.e(
                "NetworkOperationHandler_executeWithNetworkCheck",
                "Network operation aborted: ${networkError.message}"
            )
            Result.failure(Exception(networkError.message))
        } else {
            try {
                val result = operation()
                Log.d(
                    "NetworkOperationHandler_executeWithNetworkCheck",
                    "Network operation completed successfully"
                )
                Result.success(result)
            } catch (e: Exception) {
                Log.e(
                    "NetworkOperationHandler_executeWithNetworkCheck",
                    "Network operation failed: ${e.message}\n${Log.getStackTraceString(e)}"
                )
                Result.failure(e)
            }
        }
    }
}
