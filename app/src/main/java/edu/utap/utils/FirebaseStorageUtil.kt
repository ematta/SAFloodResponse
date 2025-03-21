package edu.utap.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import kotlinx.coroutines.tasks.await
import kotlin.Result

/**
 * Interface defining operations for Firebase Storage interactions.
 *
 * This interface abstracts the Firebase Storage operations,
 * allowing for different implementations and facilitating
 * testing through dependency injection.
 */
interface FirebaseStorageUtilInterface {
    /**
     * Uploads a flood report image to Firebase Storage.
     *
     * Default implementation throws NotImplementedError to allow test fakes.
     */
    suspend fun uploadFloodReportImage(context: Context, imageUri: Uri, reportId: String): Result<String> =
        throw NotImplementedError("uploadFloodReportImage is not implemented")

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
class FirebaseStorageUtil(
    private val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()
) : FirebaseStorageUtilInterface {
    init {
        // Increase upload and operation retry time to avoid session termination errors
        firebaseStorage.setMaxUploadRetryTimeMillis(60_000L)
        firebaseStorage.setMaxDownloadRetryTimeMillis(120_000L)
    }

    private val storageRef by lazy { firebaseStorage.reference }

    /**
     * Uploads an image to Firebase Storage and returns the download URL
     * @param context Android context
     * @param imageUri URI of the image to upload
     * @param userId User ID to use in the storage path
     * @return Result containing the download URL if successful, or an exception if failed
     */
    override suspend fun uploadProfileImage(
        context: Context,
        imageUri: Uri,
        userId: String
    ): Result<String> {
        val profileImagesRef = storageRef.child(
            "profile_images/$userId/${UUID.randomUUID()}.${getFileExtension(context, imageUri)}"
        )

        try {
            profileImagesRef.putFile(imageUri).await()
        } catch (e: Exception) {
            return Result.failure(e.cause ?: e)
        }
        return Result.success(profileImagesRef.downloadUrl.await().toString())
    }

    /**
     * Deletes an image from Firebase Storage
     * @param imageUrl URL of the image to delete
     * @return Result containing Unit if successful, or an exception if failed
     */
    override suspend fun deleteProfileImage(imageUrl: String): Result<Unit>{
        val imageRef = firebaseStorage.getReferenceFromUrl(imageUrl)
        imageRef.delete().await()
        return Result.success(Unit)
    }

    /**
     * Uploads a flood report image to Firebase Storage and returns the download URL
     * @param context Android context
     * @param imageUri URI of the image to upload
     * @param reportId Report ID to use in the storage path
     * @return Result containing the download URL if successful, or an exception if failed
     */
    override suspend fun uploadFloodReportImage(
        context: Context,
        imageUri: Uri,
        reportId: String
    ): Result<String>{
        val floodReportImagesRef = storageRef.child(
            "flood_reports/$reportId/${UUID.randomUUID()}.${getFileExtension(context, imageUri)}"
        )
        floodReportImagesRef.putFile(imageUri).await()
        return Result.success(floodReportImagesRef.downloadUrl.await().toString())
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
