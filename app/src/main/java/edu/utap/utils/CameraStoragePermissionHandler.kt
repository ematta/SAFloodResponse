package edu.utap.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * CameraStoragePermissionHandler
 *
 * A class responsible for handling camera and storage permission requests for Android applications.
 * It handles the runtime permissions required for taking photos and accessing storage.
 * 
 * @param activity The ComponentActivity instance used for launching permission requests.
 */
class CameraStoragePermissionHandler(private val activity: ComponentActivity) : DefaultLifecycleObserver {
    
    private var permissionRequest = registerForPermissions()
    
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        permissionRequest = registerForPermissions()
    }
    
    private var onPermissionGranted: () -> Unit = {}
    private var onPermissionDenied: () -> Unit = {}
    
    /**
     * Registers an activity result launcher for requesting camera and storage permissions.
     * The appropriate permissions are requested based on the Android SDK version.
     */
    private fun registerForPermissions() = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val permissionsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ we just need CAMERA permission
            permissions.getOrDefault(Manifest.permission.CAMERA, false) 
        } else {
            // For Android 9 and below, we need both CAMERA and storage permissions
            permissions.getOrDefault(Manifest.permission.CAMERA, false) &&
            permissions.getOrDefault(Manifest.permission.WRITE_EXTERNAL_STORAGE, false) &&
            permissions.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false)
        }
        
        if (permissionsGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }
    
    /**
     * Checks if the app has the required camera and storage permissions and requests them if necessary.
     *
     * @param onGranted Callback for when permissions are granted
     * @param onDenied Callback for when permissions are denied
     */
    fun checkAndRequestPermissions(onGranted: () -> Unit = {}, onDenied: () -> Unit = {}) {
        onPermissionGranted = onGranted
        onPermissionDenied = onDenied
        
        when {
            hasPermissions() -> onPermissionGranted()
            else -> requestPermissions()
        }
    }
    
    /**
     * Checks if the app has the required permissions for camera and storage operations.
     * For Android 10+ only camera permission is checked.
     * For Android 9 and below, camera and storage permissions are checked.
     */
    private fun hasPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        // For Android 10+ we only need CAMERA permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return cameraPermission
        }
        
        // For Android 9 and below, check storage permissions too
        val writePermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        val readPermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        return cameraPermission && writePermission && readPermission
    }
    
    /**
     * Requests the permissions required for camera and storage operations.
     * For Android 10+ only camera permission is requested.
     * For Android 9 and below, camera and storage permissions are requested.
     */
    private fun requestPermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, scoped storage eliminates the need for storage permissions
            arrayOf(Manifest.permission.CAMERA)
        } else {
            // For Android 9 and below, request both camera and storage permissions
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        
        permissionRequest.launch(permissionsToRequest)
    }
}
