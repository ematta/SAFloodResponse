package edu.utap

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.DeadObjectException
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.auth.AuthState
import edu.utap.auth.ForgotPasswordScreen
import edu.utap.auth.LoginScreen
import edu.utap.auth.RegisterScreen
import edu.utap.auth.di.ViewModelFactory
import edu.utap.auth.model.AuthViewModel
import edu.utap.flood.di.FloodViewModelFactory
import edu.utap.flood.repository.FloodReportRepository
import edu.utap.location.LocationPermissionHandler
import edu.utap.ui.components.AppHeader
import edu.utap.ui.screens.DashboardScreen
import edu.utap.ui.screens.ProfileScreen
import edu.utap.ui.screens.discussion.DiscussionListScreen
import edu.utap.ui.screens.discussion.DiscussionThreadScreen
import edu.utap.ui.screens.flood.FloodReportFormScreen
import edu.utap.ui.theme.SAFloodResponseTheme
import edu.utap.ui.viewmodel.WeatherViewModel
import edu.utap.utils.NetworkConnectivitySnackbar
import edu.utap.utils.NetworkMonitor
import edu.utap.weather.NOAAService
import edu.utap.weather.repository.WeatherRepositoryImpl
import okhttp3.OkHttpClient
import kotlin.getValue
import kotlinx.coroutines.launch

import androidx.lifecycle.lifecycleScope

/**
 * Main activity for the Flood Response application.
 * Handles authentication flow and main navigation.
 */
class MainActivity : ComponentActivity() {

    private val floodReportRepository = FloodReportRepository()

    private lateinit var locationPermissionHandler: LocationPermissionHandler
    private lateinit var networkMonitor: NetworkMonitor

    val authViewModel by viewModels<AuthViewModel> {
        ViewModelFactory(applicationContext)
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("MainActivity_onCreate",
            "Creating activity [Thread: ${Thread.currentThread().name}]")
        super.onCreate(savedInstanceState)

        // Restore auth state
        authViewModel.restoreAuthState()

        // Check cached authentication and expiration
        val cachedUser = authViewModel.getCachedUser()
        var initialIsAuthenticated = cachedUser != null && !authViewModel.isAuthExpired()
        if (!initialIsAuthenticated) {
            authViewModel.clearAuthCache()
        }

        // Initialize location permission handler first
        locationPermissionHandler = LocationPermissionHandler(this)
        lifecycle.addObserver(locationPermissionHandler)
        Log.d("MainActivity_onCreate", "Initialized location permission handler")

        // Initialize network monitor
        networkMonitor = NetworkMonitor(applicationContext)
        Log.d("MainActivity_onCreate", "Initialized network monitor")

        enableEdgeToEdge()
        Log.d("MainActivity_onCreate", "Enabled edge-to-edge mode")

        setContent {
            var showRegisterScreen by rememberSaveable { mutableStateOf(false) }
            var showForgotPasswordScreen by rememberSaveable { mutableStateOf(false) }
            var isAuthenticated by rememberSaveable { mutableStateOf(initialIsAuthenticated) }

            // Observe authentication state
            LaunchedEffect(Unit) {
                authViewModel.authState.collect { state ->
                    val newAuthState = state is AuthState.Idle.Authenticated
                    if (newAuthState != isAuthenticated) {
                        Log.i("MainActivity_authState",
                            "Authentication state changed to: $newAuthState [Thread: ${Thread.currentThread().name}]")
                    }
                    isAuthenticated = newAuthState
                }
            }

            if (isAuthenticated) {
                SAFloodResponseTheme {
                    AuthenticatedApp(
                        networkMonitor = networkMonitor,
                        locationPermissionHandler = locationPermissionHandler,
                        authViewModel = authViewModel
                    )
                }
            } else if (showRegisterScreen) {
                SAFloodResponseTheme {
                    val context = LocalContext.current
                    Scaffold(
                        topBar = {
                            AppHeader(
                                onTestScreenClick = {
                                    (context as? MainActivity)?.onTestScreenClick()
                                }
                            )
                        }
                    ) {
                        RegisterScreen(
                            authViewModel = authViewModel,
                            onNavigateToLogin = { showRegisterScreen = false },
                            onRegisterSuccess = { isAuthenticated = true }
                        )
                    }
                }
            } else if (showForgotPasswordScreen) {
                SAFloodResponseTheme {
                    val context = LocalContext.current
                    Scaffold(
                        topBar = {
                            AppHeader(
                                onTestScreenClick = {
                                    (context as? MainActivity)?.onTestScreenClick()
                                }
                            )
                        },
                        content = {
                            ForgotPasswordScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = { showForgotPasswordScreen = false }
                            )
                        }
                    )
                }
            } else {
                SAFloodResponseTheme {
                    val context = LocalContext.current
                    Scaffold(
                        topBar = {
                            AppHeader(
                                onTestScreenClick = {
                                    (context as? MainActivity)?.onTestScreenClick()
                                }
                            )
                        },
                        content = { padding: PaddingValues ->
                            LoginScreen(
                                authViewModel = authViewModel,
                                onNavigateToRegister = { showRegisterScreen = true },
                                onNavigateToForgotPassword = { showForgotPasswordScreen = true },
                                onLoginSuccess = { isAuthenticated = true }
                            )
                        }
                    
                    )
                }
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
        // Validate session on resume
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
        // No need to explicitly save state - ViewModel handles it
    }
    fun onTestScreenClick() {
        lifecycleScope.launch {
            repeat(5) { index ->
                val dummyReport = edu.utap.flood.model.FloodReport(
                    reportId = java.util.UUID.randomUUID().toString(),
                    userId = "testUser${(1..1000).random()}",
                    latitude = 29.2 + kotlin.random.Random.nextDouble() * 0.4, // San Antonio approx
                    longitude = -98.8 + kotlin.random.Random.nextDouble() * 0.5,
                    description = "Test flood report #$index",
                    photoUrls = emptyList(),
                    status = "pending",
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now(),
                    isManualLocation = kotlin.random.Random.nextBoolean(),
                    confirmedCount = kotlin.random.Random.nextInt(0, 5),
                    deniedCount = kotlin.random.Random.nextInt(0, 5),
                    severity = listOf("low", "medium", "high").random(),
                    waterDepthInches = kotlin.random.Random.nextDouble(0.0, 36.0),
                    isRoadClosed = kotlin.random.Random.nextBoolean()
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
 * Handles navigation between different screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedApp(
    networkMonitor: NetworkMonitor,
    locationPermissionHandler: LocationPermissionHandler,
    authViewModel: AuthViewModel = viewModel()
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
                val weatherViewModel = WeatherViewModel()
                DashboardScreen(
                    navController = navController,
                    locationPermissionHandler = locationPermissionHandler,
                    weatherViewModel = weatherViewModel,
                    floodReportRepository = floodReportRepository,
                    modifier = Modifier
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
                val weatherRepository = WeatherRepositoryImpl(
                    NOAAService(OkHttpClient())
                )
                FloodReportFormScreen(
                    viewModel = viewModel(
                        factory = FloodViewModelFactory(
                            context = LocalContext.current,
                            floodReportRepository = floodReportRepository,
                            weatherRepository = weatherRepository
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
