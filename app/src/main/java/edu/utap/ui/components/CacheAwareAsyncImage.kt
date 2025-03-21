package edu.utap.ui.components

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import edu.utap.utils.DefaultImageCacheProvider
import com.google.firebase.auth.FirebaseAuth

private const val TAG = "CacheAwareAsyncImage"

/**
 * A composable that displays an image from a URL or cached source.
 *
 * This component first checks if the image is available in the local cache,
 * and if so, loads it from there for faster loading and offline access.
 * Otherwise, it falls back to loading from the provided URL.
 *
 * @param photoUrl URL of the image to display (remote URL)
 * @param contentDescription Description of the image for accessibility
 * @param modifier Modifier to apply to the image
 * @param contentScale How the image should be scaled within its bounds
 * @param showLoadingIndicator Whether to show a loading indicator while the image loads
 */
@Composable
fun CacheAwareAsyncImage(
    photoUrl: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    showLoadingIndicator: Boolean = true
) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""
    
    // Log the input for debugging
    Log.d(TAG, "Loading profile image - userId: $userId, photoUrl: $photoUrl")
    
    // Check for cached image first
    val imageCacheProvider = DefaultImageCacheProvider.getInstance(context)
    val cachedImageUri = remember(userId, photoUrl) {
        if (userId.isNotEmpty()) {
            val uri = imageCacheProvider.getCachedProfileImageUri(userId)
            Log.d(TAG, "Cached image URI: ${uri?.toString() ?: "null"}")
            uri
        } else null
    }
    
    // Choose the image source - cached if available, otherwise remote URL
    val imageSource = if (cachedImageUri != null) {
        cachedImageUri
    } else if (photoUrl.isNotEmpty()) {
        photoUrl
    } else {
        null
    }
    
    Log.d(TAG, "Using image source: ${imageSource?.toString() ?: "null"}")
    
    if (imageSource != null) {
        // Use SubcomposeAsyncImage for better control over loading, success, and error states
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageSource)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        ) {
            when (painter.state) {
                is AsyncImagePainter.State.Loading -> {
                    if (showLoadingIndicator) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                is AsyncImagePainter.State.Success -> {
                    Log.d(TAG, "Image loaded successfully: $imageSource")
                    SubcomposeAsyncImageContent()
                }
                is AsyncImagePainter.State.Error -> {
                    val error = (painter.state as AsyncImagePainter.State.Error).result.throwable
                    Log.e(TAG, "Error loading image: $imageSource", error)
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "!",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> SubcomposeAsyncImageContent()
            }
        }
    } else {
        // Fallback when no source is available
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
        ) {
            Text(
                text = "No Image",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    
    // Trigger prefetching for next time if we have a URL
    LaunchedEffect(photoUrl) {
        if (photoUrl.isNotEmpty() && userId.isNotEmpty()) {
            Log.d(TAG, "Prefetching image: $photoUrl for user: $userId")
            imageCacheProvider.prefetchProfileImage(userId, photoUrl)
        }
    }
}
