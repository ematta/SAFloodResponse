package edu.utap.auth

import edu.utap.utils.ApplicationContextProvider
import edu.utap.utils.NetworkUtilsInterface

/**
 * Handles network-related operations for authentication.
 * This class provides methods to check network connectivity and handle network-dependent operations.
 */
class NetworkOperationHandler(
    private val networkUtils: NetworkUtilsInterface
) {
    /**
     * Checks if network is available and returns an error state if not.
     * @return AuthState.Error.Network if network is not available, null otherwise
     */
    fun checkNetworkAvailability(): AuthState.Error.Network? {
        return if (!networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
            AuthState.Error.Network(
                "No internet connection. Please check your network settings and try again."
            )
        } else null
    }

    /**
     * Executes a network operation with network availability check.
     * @param operation The operation to execute
     * @return Result of the operation or network error state
     */
    suspend fun <T> executeWithNetworkCheck(operation: suspend () -> T): Result<T> {
        val networkError = checkNetworkAvailability()
        return if (networkError != null) {
            Result.failure(Exception(networkError.message))
        } else {
            try {
                Result.success(operation())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
} 
