package edu.utap.user

import android.content.Context
import android.net.Uri
import edu.utap.auth.utils.ApplicationContextProvider

/**
 * A provider for FirebaseStorageUtilInterface that uses the application context
 * This helps avoid memory leaks by ensuring we don't hold references to Activity contexts
 */
class StorageUtilProvider {
    companion object {
        private val storageUtil = FirebaseStorageUtil()
        
        /**
         * Get a FirebaseStorageUtilInterface instance that uses the application context
         */
        fun getStorageUtil(): FirebaseStorageUtilInterface {
            return object : FirebaseStorageUtilInterface {
                override suspend fun uploadProfileImage(context: Context, imageUri: Uri, userId: String): Result<String> {
                    // Ignore the passed context and use application context with storageUtil
                    return storageUtil.uploadProfileImage(ApplicationContextProvider.getApplicationContext(), imageUri, userId)
                }
                
                override suspend fun deleteProfileImage(imageUrl: String): Result<Unit> {
                    return storageUtil.deleteProfileImage(imageUrl)
                }
            }
        }
    }
}