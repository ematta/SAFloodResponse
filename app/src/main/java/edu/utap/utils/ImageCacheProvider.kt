package edu.utap.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Interface for accessing image cache functionality
 * This allows for dependency injection and easier testing
 */
interface ImageCacheProviderInterface {
    /**
     * Get the ImageCacheManager instance
     */
    fun getImageCacheManager(): ImageCacheManager
    
    /**
     * Cache a profile image for a user
     */
    suspend fun cacheProfileImage(userId: String, imageUri: Uri): Boolean
    
    /**
     * Get the Uri for a cached profile image if it exists
     */
    fun getCachedProfileImageUri(userId: String): Uri?
    
    /**
     * Prefetch a profile image from a URL
     */
    suspend fun prefetchProfileImage(userId: String, imageUrl: String)
    
    /**
     * Clear a cached profile image
     */
    fun clearCachedProfileImage(userId: String): Boolean
}

/**
 * Implementation of ImageCacheProviderInterface
 * Provides a centralized point of access for image caching functionality
 */
class DefaultImageCacheProvider(
    private val context: Context
) : ImageCacheProviderInterface {

    companion object {
        private const val TAG = "ImageCacheProvider"
        @Volatile
        private var INSTANCE: DefaultImageCacheProvider? = null
        
        fun getInstance(context: Context): DefaultImageCacheProvider {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DefaultImageCacheProvider(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val icm by lazy { ImageCacheManager(context) }
    
    /**
     * Returns the [ImageCacheManager] instance used by this component.
     *
     * The [ImageCacheManager] is responsible for caching and retrieving images.
     * It provides a centralized location for image storage and retrieval,
     * improving performance by reducing the need to repeatedly download the same images.
     *
     * This method provides access to the shared [ImageCacheManager] instance.
     *
     * @return The [ImageCacheManager] instance.
     */
    override fun getImageCacheManager(): ImageCacheManager = icm
    
    /**
     * Caches a user's profile image.
     *
     * This function takes a user ID and an image URI, and attempts to cache the image
     * associated with the user. It performs the caching operation on a background thread
     * using the IO dispatcher to avoid blocking the main thread.
     *
     * @param userId The unique identifier of the user for whom the image is being cached.
     * @param imageUri The URI of the image to be cached.
     * @return `true` if the image was successfully cached, `false` otherwise.
     *
     * The function logs the start and end of the caching process for debugging purposes.
     *
     * Example Usage:
     *
     * ```kotlin
     * val userId = "user123"
     * val imageUri = Uri.parse("content://com.example.images/profile/user123.jpg")
     * lifecycleScope.launch {
     *   val success = cacheProfileImage(userId, imageUri)
     *   if (success) {
     *     Log.d("ProfileImage", "Profile image for $userId cached successfully")
     *   } else {
     *     Log.e("ProfileImage", "Failed to cache profile image for $userId")
     *   }
     * }
     * ```
     */
    override suspend fun cacheProfileImage(userId: String, imageUri: Uri): Boolean {
        Log.d(TAG, "Caching profile image for user $userId, URI: $imageUri")
        return withContext(Dispatchers.IO) {
            val result = icm.cacheProfileImage(userId, imageUri)
            Log.d(TAG, "Cache result for $userId: $result")
            result
        }
    }
    
    /**
     * Retrieves the cached profile image URI for a given user ID.
     *
     * This function checks if a profile image has been previously cached for the specified user.
     * If a cached image exists, it returns a URI pointing to the cached file.
     * Otherwise, it returns null.
     *
     * @param userId The ID of the user whose profile image URI is requested.
     * @return A Uri object representing the cached profile image if found, or null if no cached image exists.
     *
     * @see icm.getCachedProfileImage
     */
    override fun getCachedProfileImageUri(userId: String): Uri? {
        val cachedFile = icm.getCachedProfileImage(userId)
        val uri = if (cachedFile != null) Uri.fromFile(cachedFile) else null
        Log.d(TAG, "Got cached profile image URI for $userId: ${uri ?: "null"}")
        return uri
    }
    
    /**
     * This function prefetches a user's profile image from a given URL.
     *
     * It performs the image prefetching operation on the IO dispatcher to avoid blocking the main thread.
     * This allows the image to be cached in the background, potentially speeding up its display later.
     *
     * @param userId The unique identifier of the user whose profile image is being prefetched.
     * @param imageUrl The URL of the user's profile image.
     *
     * @throws Exception Any exception that might occur during the prefetching process within `icm.prefetchProfileImage(userId, imageUrl)` will be propagated.
     *
     * @see icm.prefetchProfileImage
     */
    override suspend fun prefetchProfileImage(userId: String, imageUrl: String) {
        Log.d(TAG, "Prefetching profile image for $userId from URL: $imageUrl")
        withContext(Dispatchers.IO) {
            icm.prefetchProfileImage(userId, imageUrl)
        }
    }
    
    /**
     * Clears the cached profile image associated with the given user ID.
     *
     * This function delegates the actual clearing of the cached image to the
     * `ImageCacheManager` (icm). It also logs a debug message indicating that
     * the cache clearing operation has started for the specified user.
     *
     * @param userId The unique identifier of the user whose profile image cache
     *               should be cleared. Must not be null or empty.
     * @return `true` if the cache clearing operation was successful, `false`
     *         otherwise. The success of the operation depends on the
     *         implementation of the `ImageCacheManager`.
     *
     * @see ImageCacheManager.clearCachedProfileImage
     */
    override fun clearCachedProfileImage(userId: String): Boolean {
        Log.d(TAG, "Clearing cached profile image for $userId")
        return icm.clearCachedProfileImage(userId)
    }
}
