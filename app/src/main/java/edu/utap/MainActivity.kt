package edu.utap

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.DeadObjectException
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.auth.model.AuthViewModelInterface
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.auth.AuthState
import edu.utap.auth.di.ViewModelFactory
import edu.utap.auth.model.AuthViewModel
import edu.utap.flood.di.FloodViewModelFactory
import edu.utap.flood.repository.FloodReportRepository
import edu.utap.location.LocationPermissionHandler
import edu.utap.ui.components.AppHeader
import edu.utap.ui.screens.DashboardScreen
import edu.utap.ui.screens.user.ProfileScreen
import edu.utap.ui.screens.discussion.DiscussionListScreen
import edu.utap.ui.screens.discussion.DiscussionThreadScreen
import edu.utap.ui.screens.flood.FloodReportFormScreen
import edu.utap.ui.theme.SAFloodResponseTheme
import edu.utap.ui.viewmodel.WeatherViewModel
import edu.utap.utils.NetworkConnectivitySnackbar
import edu.utap.utils.NetworkMonitor
import okhttp3.OkHttpClient
import kotlin.getValue
import kotlinx.coroutines.launch

import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import edu.utap.auth.AuthFlowManager
import edu.utap.flood.model.FloodReport
import edu.utap.navigation.NavigationManager
import edu.utap.utils.NetworkUtils
import java.util.UUID
import kotlin.random.Random

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

    private val floodReportRepository = FloodReportRepository()

    private lateinit var locationPermissionHandler: LocationPermissionHandler
    private lateinit var networkMonitor: NetworkMonitor

    val authViewModel by viewModels<AuthViewModel> {
        ViewModelFactory(applicationContext)
    }

    private lateinit var authFlowManager: AuthFlowManager
    private lateinit var navigationManager: NavigationManager

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("MainActivity_onCreate",
            "Creating activity [Thread: ${Thread.currentThread().name}]")
        super.onCreate(savedInstanceState)

        // Initialize location permission handler
        locationPermissionHandler = LocationPermissionHandler(this)
        lifecycle.addObserver(locationPermissionHandler)
        Log.d("MainActivity_onCreate", "Initialized location permission handler")

        // Initialize network monitor and utils
        networkMonitor = NetworkMonitor(applicationContext)

        // Initialize AuthFlowManager
        authFlowManager = AuthFlowManager(authViewModel)

        // Initialize NavigationManager
        navigationManager = NavigationManager(
            authFlowManager = authFlowManager,
            networkUtils = NetworkUtils,
            networkMonitor = networkMonitor,
            locationPermissionHandler = locationPermissionHandler
        )

        enableEdgeToEdge()
        Log.d("MainActivity_onCreate", "Enabled edge-to-edge mode")

        setContent {
            SAFloodResponseTheme {
                navigationManager.NavigationHost(authViewModel)
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
        Log.i("MainActivity_onDestroy", "Destroying activity [Thread: ${Thread.currentThread().name}]")
        lifecycle.removeObserver(locationPermissionHandler)
        networkMonitor.shutdown()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    fun onTestScreenClick() {
        lifecycleScope.launch {
            repeat(5) { index ->
                val dummyReport = FloodReport(
                    reportId = UUID.randomUUID().toString(),
                    userId = "testUser${(1..1000).random()}",
                    latitude = 29.2 + Random.nextDouble() * 0.4,
                    longitude = -98.8 + Random.nextDouble() * 0.5,
                    description = "Test flood report #$index",
                    photoUrls = emptyList(),
                    status = "active",
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    isManualLocation = Random.nextBoolean(),
                    confirmedCount = Random.nextInt(0, 5),
                    deniedCount = Random.nextInt(0, 5),
                    severity = listOf("low", "medium", "high").random(),
                    waterDepthInches = Random.nextDouble(0.0, 36.0),
                    isRoadClosed = Random.nextBoolean()
                )
                try {
                    Log.d("MainActivity", "Inserting dummy report: $dummyReport")
                    floodReportRepository.createReport(dummyReport)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to insert dummy report", e)
                }
            }
        }
    }
}

/**
 * Navigation routes for the authenticated part of the app
 */

/**
 * Main composable for the authenticated part of the app.
 *
 * Handles navigation between dashboard, discussions, profile, and flood report screens.
 *
 * @param networkMonitor The network connectivity monitor.
 * @param locationPermissionHandler The location permission handler.
 * @param authViewModel The authentication ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedApp(
    networkUtils: NetworkUtils,
    networkMonitor: NetworkMonitor,
    locationPermissionHandler: LocationPermissionHandler,
    authViewModel: AuthViewModelInterface
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    val context = LocalContext.current

    // Redirect to login if not authenticated
    if (authState !is AuthState.Idle.Authenticated) {
        LaunchedEffect(Unit) {
            val activity = context as? Activity
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                return@LaunchedEffect
            }
            try {
                navController.navigate("login") {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            } catch (e: DeadObjectException) {
                Log.w("AuthenticatedApp", "DeadObjectException during navigation, ignoring", e)
            }
        }
        return
    }

    val firestore = FirebaseFirestore.getInstance()

    val floodReportRepository = FloodReportRepository(
        firestore = firestore
    )

    Scaffold(
        topBar = {
            AppHeader(
                onTestScreenClick = {
                    (context as? MainActivity)?.onTestScreenClick()
                }
            )
        },
        snackbarHost = {
            NetworkConnectivitySnackbar(networkMonitor = networkMonitor)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = AuthenticatedRoutes.DASHBOARD,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(AuthenticatedRoutes.DASHBOARD) {
                DashboardScreen(
                    navController = navController,
                    locationPermissionHandler = locationPermissionHandler,
                    weatherViewModel = WeatherViewModel(),
                    floodReportRepository = floodReportRepository,
                    networkUtils = NetworkUtils,
                    modifier = Modifier,
                    authViewModel = authViewModel
                )
            }

            composable(AuthenticatedRoutes.DISCUSSIONS) {
                DiscussionListScreen(
                    onThreadClick = { threadId ->
                        navController.navigate("discussions/$threadId")
                    },
                    navController = navController
                )
            }

            composable(
                route = AuthenticatedRoutes.DISCUSSIONS_THREAD,
                arguments = listOf(
                    navArgument("threadId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val threadId = backStackEntry.arguments?.getString("threadId")
                if (threadId.isNullOrEmpty()) {
                    // Handle the error: navigate back or show an error message
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                } else {
                    DiscussionThreadScreen(
                        threadId = threadId,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            composable(AuthenticatedRoutes.PROFILE) {
                ProfileScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(AuthenticatedRoutes.FLOOD_REPORT) {
                val floodReportRepository = FloodReportRepository(firestore)
                FloodReportFormScreen(
                    viewModel = viewModel(
                        factory = FloodViewModelFactory(
                            context = LocalContext.current,
                            floodReportRepository = floodReportRepository,
                            networkUtils = NetworkUtils
                        )
                    ),
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
