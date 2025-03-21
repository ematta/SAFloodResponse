package edu.utap.utils

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 *  LocationPermissionHandler
 *
 *  A class responsible for handling location permission requests and checks within an Android Activity.
 *  It simplifies the process of requesting ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions,
 *  and provides callbacks for when the permission is granted or denied.
 *
 *  This class utilizes Android's ActivityResult API for permission handling and implements
 *  DefaultLifecycleObserver to automatically re-register the permission request on Activity recreation.
 *
 *  @param activity The ComponentActivity instance that will be used for launching the permission request and checking permission status.
 */
class LocationPermissionHandler(private val activity: ComponentActivity) :
    DefaultLifecycleObserver {
    private var locationPermissionRequest = registerForPermissions()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        locationPermissionRequest = registerForPermissions()
    }
    private var onPermissionGranted: () -> Unit = {}
    private var onPermissionDenied: () -> Unit = {}

    /**
     * Registers an activity result launcher for requesting multiple permissions, specifically
     * ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION.
     *
     * This function creates and registers a launcher that will handle the result of a permissions
     * request dialog. Upon completion of the permission request, the result is evaluated:
     * - If either ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION is granted, [onPermissionGranted] is called.
     * - If neither location permission is granted, [onPermissionDenied] is called.
     *
     * Note:
     * - This function should be called within the `onCreate` method of an `AppCompatActivity` or a Fragment
     *   that is attached to an `AppCompatActivity` because it uses `activity.registerForActivityResult`.
     * - This function assumes that `onPermissionGranted()` and `onPermissionDenied()` are defined
     *   elsewhere in the class.
     * - The `activity` property is assumed to be an instance of `AppCompatActivity` and is accessible in the scope.
     *
     * @return The [androidx.activity.result.ActivityResultLauncher] instance that can be used to launch the permissions request.
     *
     * Example usage:
     * ```kotlin
     *  private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
     *
     *  override fun onCreate(savedInstanceState: Bundle?) {
     *      super.onCreate(savedInstanceState)
     *      // ...
     *      permissionLauncher = registerForPermissions()
     *      // ...
     *      // Then later when you need to request permissions:
     *      permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
     *  }
     *
     *  private fun onPermissionGranted() {
     *      // Handle the case where location permission is granted
     *      println("Location permission granted!")
     *  }
     *
     *  private fun onPermissionDenied() {
     *      // Handle the case where location permission is denied
     *      println("Location permission denied!")
     *  }
     * ```
     */
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

    /**
     * Checks if the app has location permission and requests it if necessary.
     *
     * This function first checks if the app already has the necessary location permission.
     * If it does, the `onGranted` callback is immediately invoked.
     * If the permission is not granted, it triggers a request to the user to grant the permission.
     * The actual permission request result handling should be implemented in the corresponding
     * `onRequestPermissionsResult` method in the activity or fragment that uses this function,
     * and should call either `onPermissionGranted()` or `onPermissionDenied()` based on the result.
     *
     * @param onGranted A lambda function to be executed when the location permission is already granted
     *                  or granted by the user after the request. Defaults to an empty function.
     * @param onDenied A lambda function to be executed when the location permission is not granted
     *                 or is denied by the user after the request. Defaults to an empty function.
     *
     * Example Usage:
     * ```kotlin
     * checkAndRequestLocationPermission(
     *     onGranted = {
     *         // Permission is granted, proceed with location-related operations.
     *         startLocationUpdates()
     *     },
     *     onDenied = {
     *         // Permission is denied, handle the scenario (e.g., show an explanation).
     *         showLocationPermissionDeniedMessage()
     *     }
     * )
     * ```
     *
     * Note: `hasLocationPermission()` and `requestLocationPermission()` are assumed to be
     * implemented elsewhere in the code, which handle the actual permission checks and requests
     * using the appropriate Android APIs.
     */
    fun checkAndRequestLocationPermission(onGranted: () -> Unit = {}, onDenied: () -> Unit = {}) {
        onPermissionGranted = onGranted
        onPermissionDenied = onDenied

        when {
            hasLocationPermission() -> onPermissionGranted()
            else -> requestLocationPermission()
        }
    }

    /**
     * Checks if the app has been granted either the `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` permission.
     *
     * This function verifies if the application has the necessary location permissions to access either precise or approximate location data.
     * It uses `ContextCompat.checkSelfPermission` to check the status of both `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` permissions.
     *
     * @return `true` if either `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` permission is granted, `false` otherwise.
     *
     * @param activity The activity context used to check the permissions.
     * @throws IllegalArgumentException if activity parameter is null.
     * @see ContextCompat.checkSelfPermission
     * @see Manifest.permission.ACCESS_FINE_LOCATION
     * @see Manifest.permission.ACCESS_COARSE_LOCATION
     */
    private fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
        activity,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Requests location permission from the user.
     *
     * This function launches a request for the following permissions:
     * - [Manifest.permission.ACCESS_FINE_LOCATION]: Grants access to precise location using GPS, Wi-Fi, cell towers, etc.
     * - [Manifest.permission.ACCESS_COARSE_LOCATION]: Grants access to approximate location using network or cell tower triangulation.
     *
     * The result of the permission request will be handled by the [locationPermissionRequest] callback,
     * which is expected to be initialized elsewhere in the code.
     *
     * Note: This function assumes that [locationPermissionRequest] is properly initialized as an
     * [ActivityResultLauncher] for permission requests before calling this method.
     *
     * Example Usage:
     * ```
     * // In an Activity or Fragment:
     * private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
     *
     * override fun onCreate(savedInstanceState: Bundle?) {
     *     super.onCreate(savedInstanceState)
     *     // ...
     *
     *     locationPermissionRequest = registerForActivityResult(
     *         ActivityResultContracts.RequestMultiplePermissions()
     *     ) { permissions ->
     *         when {
     *             permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
     *                 // Precise location access granted.
     *             }
     *             permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
     *                 // Only approximate location access granted.
     *             } else -> {
     *                 // No location access granted.
     *             }
     *         }
     *     }
     *     requestLocationPermission()
     * }
     *
     * private fun requestLocationPermission() {
     *       // ... as in the original code
     * }
     * ```
     */
    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}
