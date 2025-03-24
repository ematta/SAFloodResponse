package edu.utap.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class LocationPermissionHandler(private val activity: ComponentActivity) : DefaultLifecycleObserver {
    private var locationPermissionRequest = registerForPermissions()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        locationPermissionRequest = registerForPermissions()
    }
    private var onPermissionGranted: () -> Unit = {}
    private var onPermissionDenied: () -> Unit = {}

    private fun registerForPermissions() = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Precise or approximate location access granted
                onPermissionGranted()
            }
            else -> {
                // No location access granted
                onPermissionDenied()
            }
        }
    }

    fun checkAndRequestLocationPermission(
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ) {
        onPermissionGranted = onGranted
        onPermissionDenied = onDenied

        when {
            hasLocationPermission() -> onPermissionGranted()
            else -> requestLocationPermission()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }
}