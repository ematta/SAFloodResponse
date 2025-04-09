package edu.utap.navigation

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.auth.AuthFlowManager
import edu.utap.auth.AuthState
import edu.utap.flood.model.FloodReport
import edu.utap.flood.repository.FloodReportRepository
import edu.utap.ui.components.AppHeader
import edu.utap.ui.screens.DashboardScreen
import edu.utap.ui.screens.ProfileScreen
import edu.utap.ui.screens.discussion.DiscussionListScreen
import edu.utap.ui.screens.discussion.DiscussionThreadScreen
import edu.utap.ui.screens.flood.FloodReportFormScreen
import edu.utap.ui.viewmodel.WeatherViewModel
import edu.utap.utils.NetworkConnectivitySnackbar
import edu.utap.utils.NetworkMonitor
import edu.utap.utils.NetworkUtils
import edu.utap.weather.NOAAService
import edu.utap.weather.repository.WeatherRepositoryImpl
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

class NavigationManager(
    private val authFlowManager: AuthFlowManager,
    private val networkUtils: NetworkUtils,
    private val networkMonitor: NetworkMonitor,
    private val locationPermissionHandler: edu.utap.location.LocationPermissionHandler
) {
    @Composable
    fun NavigationHost() {
        val navController = rememberNavController()
        val context = LocalContext.current

        val isAuthenticated by authFlowManager.isAuthenticated.collectAsState()

        LaunchedEffect(isAuthenticated) {
            if (!isAuthenticated) {
                try {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                } catch (e: Exception) {
                    Log.w("NavigationManager", "Navigation error: ${e.message}")
                }
            }
        }

        Scaffold(
            topBar = {
                AppHeader(
                    onTestScreenClick = {
                        (context as? Activity)?.let { activity: Activity ->
                            if (activity is edu.utap.MainActivity) {
                                activity.onTestScreenClick()
                            }
                        }
                    }
                )
            },
            snackbarHost = {
                NetworkConnectivitySnackbar(networkMonitor = networkMonitor)
            }
        ) { paddingValues: PaddingValues ->
            NavHost(
                navController = navController,
                startDestination = if (isAuthenticated) AuthenticatedRoutes.DASHBOARD else "login",
                modifier = Modifier.padding(paddingValues)
            ) {
                if (isAuthenticated) {
                    addAuthenticatedGraph(navController)
                } else {
                    addAuthGraph(navController)
                }
            }
        }
    }

    private fun NavGraphBuilder.addAuthenticatedGraph(navController: NavHostController) {
        val firestore = FirebaseFirestore.getInstance()
        val floodReportRepository = FloodReportRepository(firestore)

        composable(AuthenticatedRoutes.DASHBOARD) {
            val weatherViewModel = WeatherViewModel()
            DashboardScreen(
                navController = navController,
                locationPermissionHandler = locationPermissionHandler,
                weatherViewModel = weatherViewModel,
                floodReportRepository = floodReportRepository,
                networkUtils = networkUtils,
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
            val weatherRepository = WeatherRepositoryImpl(
                NOAAService(OkHttpClient())
            )
            FloodReportFormScreen(
                viewModel = viewModel(
                    factory = edu.utap.flood.di.FloodViewModelFactory(
                        context = LocalContext.current,
                        floodReportRepository = floodReportRepository,
                        weatherRepository = weatherRepository,
                        networkUtils = networkUtils
                    )
                ),
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }

    private fun NavGraphBuilder.addAuthGraph(navController: androidx.navigation.NavHostController) {
        composable("login") {
            edu.utap.auth.LoginScreen(
                authViewModel = authFlowManager.authViewModel,
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onNavigateToForgotPassword = {
                    navController.navigate("forgotPassword")
                },
                onLoginSuccess = {
                    navController.navigate(AuthenticatedRoutes.DASHBOARD) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            edu.utap.auth.RegisterScreen(
                authViewModel = authFlowManager.authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(AuthenticatedRoutes.DASHBOARD) {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        composable("forgotPassword") {
            edu.utap.auth.ForgotPasswordScreen(
                viewModel = authFlowManager.authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Routes for authenticated navigation graph.
 */
object AuthenticatedRoutes {
    const val DASHBOARD = "dashboard"
    const val DISCUSSIONS = "discussions"
    const val DISCUSSIONS_THREAD = "discussions/{threadId}"
    const val PROFILE = "profile"
    const val FLOOD_REPORT = "floodReport"
}
