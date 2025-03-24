package edu.utap.user

/**
 * Provider for Firebase Storage utility functions.
 * 
 * This singleton object provides access to the FirebaseStorageUtilInterface implementation.
 * It follows the service locator pattern, allowing the actual implementation
 * to be swapped out, particularly useful for testing with mock implementations.
 */
object StorageUtilProvider {
    // Default implementation of storage utilities
    private var storageUtil: FirebaseStorageUtilInterface = FirebaseStorageUtil()
    
    /**
     * Gets the current Firebase Storage utilities implementation.
     * 
     * @return The current FirebaseStorageUtilInterface implementation
     */
    fun getStorageUtil(): FirebaseStorageUtilInterface {
        return storageUtil
    }
    
    /**
     * Sets a custom Firebase Storage utilities implementation.
     * 
     * This method is primarily used for testing to inject mock implementations.
     * 
     * @param util The FirebaseStorageUtilInterface implementation to use
     */
    // For testing purposes only
    fun setStorageUtil(util: FirebaseStorageUtilInterface) {
        storageUtil = util
    }
}