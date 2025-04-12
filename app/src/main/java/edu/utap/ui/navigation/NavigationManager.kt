package edu.utap.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.auth.AuthFlowManager
import edu.utap.auth.AuthState
import edu.utap.di.FloodViewModelFactory
import edu.utap.repository.FloodReportRepository
import edu.utap.ui.NetworkConnectivitySnackbar
import edu.utap.ui.components.AppHeader
import edu.utap.ui.components.BottomNavigationBar
import edu.utap.ui.screens.DashboardScreen
import edu.utap.ui.screens.ForgotPasswordScreen
import edu.utap.ui.screens.LoginScreen
import edu.utap.ui.screens.RegisterScreen
import edu.utap.ui.screens.discussion.DiscussionListScreen
import edu.utap.ui.screens.discussion.DiscussionThreadScreen
import edu.utap.ui.screens.flood.FloodReportFormScreen
import edu.utap.ui.screens.ProfileScreen
import edu.utap.ui.viewmodel.WeatherViewModel
import edu.utap.utils.LocationPermissionHandler
import edu.utap.utils.NetworkMonitor
import edu.utap.utils.NetworkUtilsInterface

class NavigationManager(
    private val authFlowManager: AuthFlowManager,
    private val networkUtils: NetworkUtilsInterface,
    private val networkMonitor: NetworkMonitor,
    private val locationPermissionHandler: LocationPermissionHandler
) {
    @Composable
    fun NavigationHost() {
        val navController = rememberNavController()

        val isAuthenticated by authFlowManager.isAuthenticated.collectAsState()

        Scaffold(
            topBar = {
                AppHeader(navController)
            },
            snackbarHost = {
                NetworkConnectivitySnackbar(networkMonitor = networkMonitor)
            },
            bottomBar = {
                BottomNavigationBar(navController)
            }
        ) { paddingValues: PaddingValues ->
            NavHost(
                navController = navController,
                startDestination = OpenRoutes.DASHBOARD,
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
            FloodReportFormScreen(
                viewModel = viewModel(
                    factory = FloodViewModelFactory(
                        context = LocalContext.current,
                        floodReportRepository = floodReportRepository,
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
        val firestore = FirebaseFirestore.getInstance()
        val floodReportRepository = FloodReportRepository(firestore)
        composable(OpenRoutes.LOGIN) {
            LoginScreen(
                authViewModel = authFlowManager.authViewModel,
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onNavigateToForgotPassword = {
                    navController.navigate("forgotPassword")
                },
                onLoginSuccess = {
                    navController.navigate(OpenRoutes.DASHBOARD) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable(OpenRoutes.REGISTER) {
            RegisterScreen(
                authViewModel = authFlowManager.authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(OpenRoutes.DASHBOARD) {
                        popUpTo(OpenRoutes.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        composable(OpenRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                viewModel = authFlowManager.authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(OpenRoutes.DASHBOARD) {
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
    }
}

/**
 * Routes for authenticated navigation graph.
 */
object AuthenticatedRoutes {
    const val DISCUSSIONS = "discussions"
    const val DISCUSSIONS_THREAD = "discussions/{threadId}"
    const val PROFILE = "profile"
    const val FLOOD_REPORT = "floodReport"
}

/**
 * Routes for open navigation graph.
 */
object OpenRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgotPassword"
    const val DASHBOARD = "dashboard"
}
