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
import edu.utap.di.FloodViewModelFactory
import edu.utap.repository.FloodReportRepository
import edu.utap.ui.NetworkConnectivitySnackbar
import edu.utap.ui.components.AppHeader
import edu.utap.ui.components.BottomNavigationBar
import edu.utap.ui.screens.DashboardScreen
import edu.utap.ui.screens.auth.ForgotPasswordScreen
import edu.utap.ui.screens.auth.LoginScreen
import edu.utap.ui.screens.auth.RegisterScreen
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
                addAuthGraph(navController)
            }
        }
    }

    private fun NavGraphBuilder.addAuthGraph(navController: NavHostController, authFlowManager: AuthFlowManager = this@NavigationManager.authFlowManager) {
        val firestore = FirebaseFirestore.getInstance()
        val floodReportRepository = FloodReportRepository(firestore)

        composable(OpenRoutes.LOGIN) {
            LoginScreen(
                authViewModel = authFlowManager.authViewModel,
                onNavigateToRegister = {
                    navController.navigate(OpenRoutes.REGISTER)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(OpenRoutes.FORGOT_PASSWORD)
                },
                onLoginSuccess = {
                    navController.navigate(OpenRoutes.DASHBOARD) {
                        popUpTo(OpenRoutes.LOGIN) { inclusive = true }
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
                    navController.navigate(OpenRoutes.LOGIN) {
                        popUpTo("register") { inclusive = true }
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
            DashboardScreen(
                navController = navController,
                locationPermissionHandler = locationPermissionHandler,
                weatherViewModel = WeatherViewModel(),
                floodReportRepository = floodReportRepository,
                networkUtils = networkUtils,
                modifier = Modifier
            )
        }

        composable(AuthenticatedRoutes.DISCUSSIONS) {
            DiscussionListScreen(
                onThreadClick = { threadId ->
                    navController.navigate("${AuthenticatedRoutes.DISCUSSIONS}/$threadId")
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
            val authState by authFlowManager.authViewModel.authState.collectAsState()

            if (authState is edu.utap.auth.AuthState.Success) {
                // User is authenticated, show the report form
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
            } else {
                // User is not authenticated, redirect to login
                // Use LaunchedEffect to navigate outside of composition
                LaunchedEffect(authState) {
                    navController.navigate(OpenRoutes.LOGIN) {
                        // Optional: Clear back stack up to dashboard or handle as needed
                        popUpTo(OpenRoutes.DASHBOARD)
                    }
                }
            }
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
