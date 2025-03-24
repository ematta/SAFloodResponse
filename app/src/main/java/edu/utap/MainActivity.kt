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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import edu.utap.auth.utils.NetworkConnectivitySnackbar
import edu.utap.auth.AuthViewModel
import edu.utap.auth.ForgotPasswordScreen
import edu.utap.auth.LoginScreen
import edu.utap.auth.RegisterScreen
import edu.utap.auth.db.DatabaseInitializer
import edu.utap.auth.di.ViewModelFactory
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.lifecycleScope
import edu.utap.auth.utils.NetworkMonitor
import edu.utap.location.LocationPermissionHandler
import kotlinx.coroutines.launch
import edu.utap.ui.theme.SAFloodResponseTheme
import edu.utap.ui.theme.Dimensions
import edu.utap.ui.components.AppBottomNavigation
import edu.utap.ui.components.AppHeader
import edu.utap.user.ProfileScreen
import edu.utap.ui.screens.DashboardScreen

class MainActivity : ComponentActivity() {
    private lateinit var locationPermissionHandler: LocationPermissionHandler
    private lateinit var networkMonitor: NetworkMonitor

    val authViewModel by viewModels<AuthViewModel> { 
        ViewModelFactory(applicationContext) 
    }

    // TODO: Remove this in production builds
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
                    isAuthenticated = state is edu.utap.auth.AuthState.Idle.Authenticated
                }
            }
            
            if (isAuthenticated) {
                SAFloodResponseTheme {
                    AuthenticatedApp(networkMonitor = networkMonitor, locationPermissionHandler = locationPermissionHandler)
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
                                onNavigateToLogin = { showForgotPasswordScreen = false },
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
                            onLoginSuccess = { isAuthenticated = true },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedApp(networkMonitor: NetworkMonitor, locationPermissionHandler: LocationPermissionHandler) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "dashboard"
    
    Scaffold(
        topBar = {
            AppHeader()
        },
        snackbarHost = {
            // Add network connectivity snackbar to show network status
            NetworkConnectivitySnackbar(networkMonitor = networkMonitor)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    navController = navController,
                    locationPermissionHandler = locationPermissionHandler
                )
            }
            
            composable("discussions") {
                // TODO: Add discussions screen
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("Discussions Screen")
                }
            }
            
            composable("emergency") {
                // TODO: Add emergency screen
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("Emergency Screen")
                }
            }
            
            composable("profile") {
                ProfileScreen(
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