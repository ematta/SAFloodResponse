package edu.utap

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import edu.utap.auth.AuthFlowManager
import edu.utap.di.ViewModelFactory
import edu.utap.ui.navigation.NavigationManager
import edu.utap.ui.theme.SAFloodResponseTheme
import edu.utap.utils.NetworkUtilsImpl
import edu.utap.ui.viewmodel.AuthViewModel
import edu.utap.ui.viewmodel.UserViewModel
import edu.utap.utils.CameraStoragePermissionHandler
import edu.utap.utils.FirebaseStorageUtil
import edu.utap.utils.FirebaseStorageUtilInterface
import edu.utap.utils.LocationPermissionHandler
import edu.utap.utils.LocationUtils
import edu.utap.utils.NetworkMonitor
import kotlin.getValue

/**
 * Main [ComponentActivity] for the Flood Response application.
 *
 * Handles:
 * - Authentication flow (login, register, forgot password)
 * - Navigation between screens
 * - Initialization of location permissions and network monitoring
 * - Theme setup
 */
class MainActivity : ComponentActivity() {
    private lateinit var locationPermissionHandler: LocationPermissionHandler
    private lateinit var cameraStoragePermissionHandler: CameraStoragePermissionHandler
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var storageUtil: FirebaseStorageUtilInterface

    private lateinit var authFlowManager: AuthFlowManager
    private lateinit var navigationManager: NavigationManager

    val authViewModel by viewModels<AuthViewModel> {
        ViewModelFactory(
            context = applicationContext
        )
    }

    val userViewModel by viewModels<UserViewModel> {
        ViewModelFactory(
            context = applicationContext
        )
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(
            "MainActivity_onCreate",
            "Creating activity [Thread: ${Thread.currentThread().name}]"
        )
        super.onCreate(savedInstanceState)

        locationPermissionHandler = LocationPermissionHandler(this)
        lifecycle.addObserver(locationPermissionHandler)
        Log.d("MainActivity_onCreate", "Initialized location permission handler")

        cameraStoragePermissionHandler = CameraStoragePermissionHandler(this)
        lifecycle.addObserver(cameraStoragePermissionHandler)
        Log.d("MainActivity_onCreate", "Initialized camera and storage permission handler")

        networkMonitor = NetworkMonitor(applicationContext, NetworkUtilsImpl())

        authFlowManager = AuthFlowManager(authViewModel)

        storageUtil = FirebaseStorageUtil()

        navigationManager = NavigationManager(
            authFlowManager = authFlowManager,
            networkUtils = NetworkUtilsImpl(),
            networkMonitor = networkMonitor,
            locationPermissionHandler = locationPermissionHandler,
            storageUtil = storageUtil,
            cameraStoragePermissionHandler = cameraStoragePermissionHandler
        )

        enableEdgeToEdge()
        Log.d("MainActivity_onCreate", "Enabled edge-to-edge mode")

        setContent {
            SAFloodResponseTheme {
                navigationManager.NavigationHost()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i("MainActivity_onStart", "Starting activity [Thread: ${Thread.currentThread().name}]")
    }

    override fun onResume() {
        super.onResume()
        Log.i("MainActivity_onResume", "Resuming activity [Thread: ${Thread.currentThread().name}]")
        authViewModel.checkAuthState()
    }

    override fun onPause() {
        super.onPause()
        Log.i("MainActivity_onPause", "Pausing activity [Thread: ${Thread.currentThread().name}]")
    }

    override fun onStop() {
        super.onStop()
        Log.i("MainActivity_onStop", "Stopping activity [Thread: ${Thread.currentThread().name}]")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(
            "MainActivity_onDestroy",
            "Destroying activity [Thread: ${Thread.currentThread().name}]"
        )
        lifecycle.removeObserver(locationPermissionHandler)
        lifecycle.removeObserver(cameraStoragePermissionHandler)
        networkMonitor.shutdown()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }
}
