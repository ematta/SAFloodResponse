package edu.utap.flood.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

/**
 * Utility class for handling location services and permissions.
 *
 * This class provides methods for:
 * - Checking location permissions
 * - Requesting location updates
 * - Getting the last known location
 * - Managing location settings
 */
class LocationUtils(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationRequest: LocationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        10000
    )
        .setMinUpdateIntervalMillis(5000)
        .build()
    private var locationCallback: LocationCallback? = null

    /**
     * Checks if the app has the required location permissions.
     *
     * @return true if all required permissions are granted, false otherwise
     */
    fun hasLocationPermissions(): Boolean = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Gets the last known location.
     *
     * @return Task containing the last known location, or null if not available
     */
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): Task<Location> = fusedLocationClient.lastLocation

    /**
     * Requests location updates and provides them through a callback.
     *
     * @param onLocationUpdate Callback function to receive location updates
     */
    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(onLocationUpdate: (Location) -> Unit) {
        if (!hasLocationPermissions()) {
            throw SecurityException("Location permissions not granted")
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    onLocationUpdate(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    /**
     * Stops requesting location updates.
     */
    fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
    }

    /**
     * Checks if location services are enabled.
     *
     * @return Task containing whether location services are enabled
     */
    fun isLocationEnabled(): Task<LocationSettingsResponse> {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        return LocationServices.getSettingsClient(context)
            .checkLocationSettings(builder.build())
    }
}
