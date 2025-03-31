package edu.utap

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.auth.AuthState
import edu.utap.auth.ForgotPasswordScreen
import edu.utap.auth.LoginScreen
import edu.utap.auth.RegisterScreen
import edu.utap.auth.di.ViewModelFactory
import edu.utap.auth.model.AuthViewModel
import edu.utap.db.AppDatabase
import edu.utap.flood.di.FloodViewModelFactory
import edu.utap.flood.repository.FloodReportRepository
import edu.utap.location.LocationPermissionHandler
import edu.utap.ui.components.AppHeader
import edu.utap.ui.screens.DashboardScreen
import edu.utap.ui.screens.FloodMapTestScreen
import edu.utap.ui.screens.ProfileScreen
import edu.utap.ui.screens.flood.FloodReportFormScreen
import edu.utap.ui.theme.SAFloodResponseTheme
import edu.utap.ui.viewmodel.WeatherViewModel
import edu.utap.utils.NetworkConnectivitySnackbar
import edu.utap.utils.NetworkMonitor
import kotlin.getValue

/**
 * Main activity for the Flood Response application.
 * Handles authentication flow and main navigation.
 */
class MainActivity : ComponentActivity() {
    private lateinit var locationPermissionHandler: LocationPermissionHandler
    private lateinit var networkMonitor: NetworkMonitor

    val authViewModel by viewModels<AuthViewModel> {
        ViewModelFactory(applicationContext)
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize location permission handler first
        locationPermissionHandler = LocationPermissionHandler(this)
        lifecycle.addObserver(locationPermissionHandler)

        // Initialize network monitor
        networkMonitor = NetworkMonitor(applicationContext)

        enableEdgeToEdge()
        setContent {
            var showRegisterScreen by rememberSaveable { mutableStateOf(false) }
            var showForgotPasswordScreen by rememberSaveable { mutableStateOf(false) }
            var isAuthenticated by rememberSaveable { mutableStateOf(false) }

            // Observe authentication state
            LaunchedEffect(Unit) {
                authViewModel.authState.collect { state ->
                    isAuthenticated = state is AuthState.Idle.Authenticated
                }
            }

            if (isAuthenticated) {
                SAFloodResponseTheme {
                    AuthenticatedApp(
                        networkMonitor = networkMonitor,
                        locationPermissionHandler = locationPermissionHandler
                    )
                }
            } else if (showRegisterScreen) {
                SAFloodResponseTheme {
                    Scaffold(
                        topBar = {
                            AppHeader()
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
                    Scaffold(
                        topBar = {
                            AppHeader()
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
                    Scaffold(
                        topBar = {
                            AppHeader()
                        }
                    ) {
                        LoginScreen(
                            authViewModel = authViewModel,
                            onNavigateToRegister = { showRegisterScreen = true },
                            onNavigateToForgotPassword = { showForgotPasswordScreen = true },
                            onLoginSuccess = { isAuthenticated = true }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Navigation routes for the authenticated part of the app
 */
object AuthenticatedRoutes {
    const val DASHBOARD = "dashboard"
    const val DISCUSSIONS = "discussions"
    const val EMERGENCY = "emergency"
    const val PROFILE = "profile"
    const val FLOOD_REPORT = "flood_report"
}

/**
 * Main composable for the authenticated part of the app.
 * Handles navigation between different screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedApp(
    networkMonitor: NetworkMonitor,
    locationPermissionHandler: LocationPermissionHandler
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: AuthenticatedRoutes.DASHBOARD

    val applicationContext = LocalContext.current.applicationContext
    val floodReportDao = AppDatabase.getDatabase(applicationContext).floodReportDao()
    val firestore = FirebaseFirestore.getInstance()

    val floodReportRepository = FloodReportRepository(
        floodReportDao = floodReportDao,
        firestore = firestore
    )

    Scaffold(
        topBar = {
            AppHeader(
                onTestScreenClick = {
                    navController.navigate("flood_map_test")
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
                // TODO: Implement discussions screen
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("Discussions Screen")
                }
            }

            composable(AuthenticatedRoutes.EMERGENCY) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = { navController.navigate(AuthenticatedRoutes.FLOOD_REPORT) },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text("Report Flood")
                    }
                }
            }

            composable(AuthenticatedRoutes.PROFILE) {
                ProfileScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("flood_map_test") {
                FloodMapTestScreen(
                    navController = navController
                )
            }

            composable(AuthenticatedRoutes.FLOOD_REPORT) {
                FloodReportFormScreen(
                    viewModel = viewModel(
                        factory = FloodViewModelFactory(context = LocalContext.current)
                    ),
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SAFloodResponseTheme {
        Greeting("Android")
    }
}
