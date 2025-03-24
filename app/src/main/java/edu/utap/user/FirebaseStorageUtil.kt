package edu.utap.user

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Interface defining operations for Firebase Storage interactions.
 * 
 * This interface abstracts the Firebase Storage operations,
 * allowing for different implementations and facilitating
 * testing through dependency injection.
 */
interface FirebaseStorageUtilInterface {
    /**
     * Uploads a profile image to Firebase Storage.
     * 
     * @param context Android context
     * @param imageUri The URI of the image to upload
     * @param userId The user ID to use in the storage path
     * @return Result containing the download URL of the uploaded image or an error
     */
    suspend fun uploadProfileImage(context: Context, imageUri: Uri, userId: String): Result<String>
    
    /**
     * Deletes a profile image from Firebase Storage.
     * 
     * @param imageUrl The URL of the image to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteProfileImage(imageUrl: String): Result<Unit>
}

/**
 * Implementation of FirebaseStorageUtilInterface for handling profile image storage operations.
 * 
 * This class provides methods to upload and delete profile images using Firebase Storage.
 * It generates unique filenames for uploaded images to prevent collisions and
 * handles the conversion between local URIs and Firebase Storage download URLs.
 */
class FirebaseStorageUtil(private val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()) : FirebaseStorageUtilInterface {
    private val storageRef by lazy { firebaseStorage.reference }
    
    /**
     * Uploads an image to Firebase Storage and returns the download URL
     * @param context Android context
     * @param imageUri URI of the image to upload
     * @param userId User ID to use in the storage path
     * @return Result containing the download URL if successful, or an exception if failed
     */
    override suspend fun uploadProfileImage(context: Context, imageUri: Uri, userId: String): Result<String> {
        return try {
            // Create a reference to the profile image location
            val profileImagesRef = storageRef.child("profile_images/$userId/${UUID.randomUUID()}.${getFileExtension(context, imageUri)}")
            
            // Upload the file
            val uploadTask = profileImagesRef.putFile(imageUri).await()
            
            // Get the download URL
            val downloadUrl = profileImagesRef.downloadUrl.await().toString()
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Deletes an image from Firebase Storage
     * @param imageUrl URL of the image to delete
     * @return Result containing Unit if successful, or an exception if failed
     */
    override suspend fun deleteProfileImage(imageUrl: String): Result<Unit> {
        return try {
            // Create a reference to the file to delete
            val imageRef = firebaseStorage.getReferenceFromUrl(imageUrl)
            
            // Delete the file
            imageRef.delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets the file extension from a URI
     * @param context Android context
     * @param uri URI to get the extension from
     * @return File extension or default to "jpg"
     */
    private fun getFileExtension(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        
        val mimeType = contentResolver.getType(uri)
        return if (mimeType != null) {
            mimeTypeMap.getExtensionFromMimeType(mimeType) ?: "jpg"
        } else {
            "jpg"
        }
    }
}