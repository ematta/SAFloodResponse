package edu.utap.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import coil.decode.DataSource
import androidx.core.graphics.drawable.toBitmap
/**
 * Manages caching of profile images for offline access.
 * 
 * This class provides functionality to:
 * 1. Save profile images to disk cache
 * 2. Retrieve cached profile images
 * 3. Configure Coil image loader with proper caching policies
 */
class ImageCacheManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageCacheManager"
        private const val CACHE_FOLDER = "profile_images"
        private const val CACHE_SIZE = 50 * 1024 * 1024 // 50MB
    }
    
    // Directory for profile image cache
    private val cacheDir by lazy { File(context.cacheDir, CACHE_FOLDER).apply { 
        if (!exists()) {
            val dirCreated = mkdirs()
            Log.d(TAG, "Cache directory created: $dirCreated, path: $absolutePath")
        } else {
            Log.d(TAG, "Cache directory already exists at: $absolutePath")
        }
    }}
    
    // Custom ImageLoader with disk cache configuration
    val imageLoader by lazy { 
        ImageLoader.Builder(context)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir)
                    .maxSizeBytes(CACHE_SIZE.toLong())
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .respectCacheHeaders(false) // Ignore cache headers from network for our profile images
            .crossfade(true) // Add crossfade effect for smoother loading
            .build()
    }
    
    /**
     * Get a cached file for a user's profile image
     * 
     * @param userId The ID of the user
     * @return The cached file or null if no cache exists
     */
    fun getCachedProfileImage(userId: String): File? {
        val file = File(cacheDir, getProfileCacheFileName(userId))
        val exists = file.exists() && file.length() > 0
        Log.d(TAG, "Checking cached profile image for $userId: exists=$exists, path=${file.absolutePath}")
        return if (exists) file else null
    }
    
    /**
     * Save a profile image to the cache
     * 
     * @param userId The ID of the user
     * @param imageUri The URI of the image to cache
     * @return True if caching was successful, false otherwise
     */
    suspend fun cacheProfileImage(userId: String, imageUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val imageFile = File(cacheDir, getProfileCacheFileName(userId))
                Log.d(TAG, "Caching image from URI $imageUri to file ${imageFile.absolutePath}")
                
                // Copy the image to our cache directory
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    FileOutputStream(imageFile).use { output ->
                        val bytesCopied = input.copyTo(output)
                        Log.d(TAG, "Copied $bytesCopied bytes to cache for user $userId")
                    }
                } ?: run {
                    Log.e(TAG, "Failed to open input stream for URI: $imageUri")
                    return@withContext false
                }
                
                Log.d(TAG, "Profile image cached successfully for user $userId, size: ${imageFile.length()} bytes")
                true
            } catch (e: IOException) {
                Log.e(TAG, "Failed to cache profile image for user $userId", e)
                false
            }
        }
    }
    
    /**
     * Save a profile image bitmap to the cache
     * 
     * @param userId The ID of the user
     * @param bitmap The bitmap to cache
     * @return True if caching was successful, false otherwise
     */
    suspend fun cacheProfileImage(userId: String, bitmap: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val imageFile = File(cacheDir, getProfileCacheFileName(userId))
                
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                
                Log.d(TAG, "Profile image bitmap cached successfully for user $userId, size: ${imageFile.length()} bytes")
                true
            } catch (e: IOException) {
                Log.e(TAG, "Failed to cache profile image bitmap for user $userId", e)
                false
            }
        }
    }
    
    /**
     * Prefetch and cache a profile image from a URL
     * 
     * @param userId The ID of the user
     * @param imageUrl The URL of the image to prefetch and cache
     */
    suspend fun prefetchProfileImage(userId: String, imageUrl: String) {
        if (imageUrl.isEmpty()) {
            Log.d(TAG, "Empty URL provided for prefetching, skipping")
            return
        }
        
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Prefetching profile image from $imageUrl for user $userId")
                
                // First, try to download directly to our custom cache file
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(Size.ORIGINAL)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .listener(
                        onSuccess = { _, result ->
                            val source = result.dataSource
                            Log.d(TAG, "Successfully loaded profile image for user $userId from $source")
                            
                            // If image was fetched from network, also save to our custom file cache
                            if (source == DataSource.NETWORK) {
                                result.drawable.toBitmap().let { bitmap ->
                                    try {
                                        val imageFile = File(cacheDir, getProfileCacheFileName(userId))
                                        FileOutputStream(imageFile).use { out ->
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                        }
                                        Log.d(TAG, "Also saved network-loaded image to custom cache: ${imageFile.absolutePath}")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to save network-loaded image to custom cache", e)
                                    }
                                }
                            }
                        },
                        onError = { _, result ->
                            Log.e(TAG, "Failed to prefetch profile image for $userId: ${result.throwable.message}", result.throwable)
                        }
                    )
                    .build()
                
                imageLoader.execute(request)
            } catch (e: Exception) {
                Log.e(TAG, "Error prefetching image for $userId", e)
            }
        }
    }
    
    /**
     * Clear cached profile image for a specific user
     * 
     * @param userId The ID of the user
     * @return True if the cache was cleared successfully, false otherwise
     */
    fun clearCachedProfileImage(userId: String): Boolean {
        try {
            val file = File(cacheDir, getProfileCacheFileName(userId))
            val result = if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Deleted cached profile image for user $userId: $deleted")
                deleted
            } else {
                Log.d(TAG, "No cached profile image exists for user $userId")
                true
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cached profile image for user $userId", e)
            return false
        }
    }
    
    /**
     * Generate a consistent filename for cached profile images
     * 
     * @param userId The ID of the user
     * @return The filename to use for caching
     */
    private fun getProfileCacheFileName(userId: String): String = "profile_$userId.jpg"
}
