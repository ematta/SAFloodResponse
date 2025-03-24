package edu.utap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import edu.utap.ui.theme.SAFloodResponseTheme
import edu.utap.user.ProfileScreen

class MainActivity : ComponentActivity() {

    val authViewModel by viewModels<AuthViewModel> { 
        ViewModelFactory(applicationContext) 
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize and log the database structure (only for debugging)
        // TODO: Remove this in production builds
        DatabaseInitializer.logDatabaseScript(this)
        
        enableEdgeToEdge()
        setContent {
            var showRegisterScreen by remember { mutableStateOf(false) }
            var showForgotPasswordScreen by remember { mutableStateOf(false) }
            var isAuthenticated by remember { mutableStateOf(false) }
            
            // Observe authentication state
            LaunchedEffect(Unit) {
                authViewModel.authState.collect { state ->
                    isAuthenticated = state is edu.utap.auth.AuthState.Authenticated
                }
            }
            
            if (isAuthenticated) {
                SAFloodResponseTheme {
                    AuthenticatedApp()
                }
            } else if (showRegisterScreen) {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = { showRegisterScreen = false },
                    onRegisterSuccess = { isAuthenticated = true }
                )
            } else if (showForgotPasswordScreen) {
                ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { showForgotPasswordScreen = false }
                )
            } else {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "home"
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = {
                        if (currentRoute != "home") {
                            navController.navigate("home") {
                                popUpTo("home") {
                                    inclusive = true
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                
                NavigationBarItem(
                    selected = currentRoute == "profile",
                    onClick = {
                        if (currentRoute != "profile") {
                            navController.navigate("profile") {
                                popUpTo("profile") {
                                    inclusive = true
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        },
        snackbarHost = {
            // Add network connectivity snackbar to show network status
            NetworkConnectivitySnackbar()
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                Box(modifier = Modifier.fillMaxSize()) {
                    Greeting("SA Flood Response User")
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