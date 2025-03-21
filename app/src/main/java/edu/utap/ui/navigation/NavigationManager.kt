package edu.utap.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.auth.AuthFlowManager
import edu.utap.di.FloodViewModelFactory
import edu.utap.di.ViewModelFactory
import edu.utap.models.FirestoreUser
import edu.utap.repository.FloodReportRepository
import edu.utap.ui.NetworkConnectivitySnackbar
import edu.utap.ui.components.AppHeader
import edu.utap.ui.components.BottomNavigationBar
import edu.utap.ui.screens.admin.AdminScreen
import edu.utap.ui.screens.dashboard.DashboardScreen
import edu.utap.ui.screens.discussion.DiscussionListScreen
import edu.utap.ui.screens.discussion.DiscussionThreadScreen
import edu.utap.ui.screens.flood.FloodReportFormScreen
import edu.utap.ui.screens.profile.ProfileScreen
import edu.utap.ui.screens.flood.FloodReportDetailScreen
import edu.utap.ui.theme.SAFloodResponseTheme
import edu.utap.ui.viewmodel.FloodReportViewModel
import edu.utap.ui.viewmodel.UserViewModel
import edu.utap.utils.CameraStoragePermissionHandler
import edu.utap.utils.FirebaseStorageUtilInterface
import edu.utap.utils.LocationPermissionHandler
import edu.utap.utils.NetworkMonitor
import edu.utap.utils.NetworkUtilsInterface
import edu.utap.utils.RoleUtils

class NavigationManager(
    private val authFlowManager: AuthFlowManager,
    private val networkUtils: NetworkUtilsInterface,
    private val networkMonitor: NetworkMonitor,
    private val locationPermissionHandler: LocationPermissionHandler,
    private val storageUtil: FirebaseStorageUtilInterface,
    private val cameraStoragePermissionHandler: CameraStoragePermissionHandler
) {
    @Composable
    fun NavigationHost() {
        // Request location on app start and update UserViewModel
        val userViewModel: UserViewModel = ViewModelFactory(context = LocalContext.current).create(UserViewModel::class.java)
        LaunchedEffect(Unit) {
            locationPermissionHandler.checkAndRequestLocationPermission(
                onGranted = { userViewModel.fetchCurrentLocation() }
            )
        }
        val navController = rememberNavController()

        // Track current route to conditionally show global header
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val isAuthenticated by authFlowManager.isAuthenticated.collectAsState()

        Scaffold(
            topBar = {
                if (currentRoute != AuthenticatedRoutes.PROFILE) {
                    AppHeader(navController, authFlowManager)
                }
            },
            snackbarHost = {
                NetworkConnectivitySnackbar(networkMonitor = networkMonitor)
            },
            bottomBar = {
                BottomNavigationBar(navController)
            }
        ) { paddingValues: PaddingValues ->
            // Recreate NavHost when authentication state changes
            key(isAuthenticated) {
                NavHost(
                    navController = navController,
                    startDestination = OpenRoutes.DASHBOARD,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    addAuthenticatedGraph(navController)
                    addGraph(navController)
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
                navController = navController,
                floodReportViewModel = viewModel(
                    factory = FloodViewModelFactory(
                        context = LocalContext.current,
                        floodReportRepository = floodReportRepository,
                        networkUtils = networkUtils,
                        storageUtil = storageUtil
                    )
                )
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateAdmin = { navController.navigate(AuthenticatedRoutes.ADMIN) }
            )
        }

        composable(AuthenticatedRoutes.ADMIN) {
            val adminFirestore = FirebaseFirestore.getInstance()
            val floodRepo = FloodReportRepository(adminFirestore)
            val adminViewModel: FloodReportViewModel = viewModel(
                factory = FloodViewModelFactory(
                    context = LocalContext.current,
                    floodReportRepository = floodRepo,
                    networkUtils = networkUtils,
                    storageUtil = storageUtil
                )
            )
            AdminScreen(viewModel = adminViewModel, onNavigateBack = { navController.popBackStack() })
        }

        composable(AuthenticatedRoutes.FLOOD_REPORT_ID) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId")
            if (reportId.isNullOrEmpty()) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            } else {
                FloodReportDetailScreen(
                    reportId = reportId,
                    viewModel = viewModel(
                        factory = FloodViewModelFactory(
                            context = LocalContext.current,
                            floodReportRepository = floodReportRepository,
                            networkUtils = networkUtils,
                            storageUtil = storageUtil
                        )
                    ),
                    onBack = {
                        navController.popBackStack()
                    },
                    navController = navController
                )
            }
        }

        composable(AuthenticatedRoutes.FLOOD_REPORT) @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) {
            // Request camera permissions before showing the screen
            var cameraPermissionsGranted by remember { mutableStateOf(false) }
            var permissionsRequested by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                if (!permissionsRequested) {
                    permissionsRequested = true
                    cameraStoragePermissionHandler.checkAndRequestPermissions(
                        onGranted = { cameraPermissionsGranted = true },
                        onDenied = { /* Handle permission denied case */ }
                    )
                }
            }
            
            FloodReportFormScreen(
                viewModel = viewModel(
                    factory = FloodViewModelFactory(
                        context = LocalContext.current,
                        floodReportRepository = floodReportRepository,
                        networkUtils = networkUtils,
                        storageUtil = storageUtil
                    )
                ),
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }

    private fun NavGraphBuilder.addGraph(navController: androidx.navigation.NavHostController) {
        val firestore = FirebaseFirestore.getInstance()
        val floodReportRepository = FloodReportRepository(firestore)

        composable(OpenRoutes.LOGIN) {
            val providers = listOf(
                    AuthUI.IdpConfig.EmailBuilder().build()
                )
            val launcher =
                rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
                    if (result?.resultCode == android.app.Activity.RESULT_OK) {
                        // Sync or create Firestore user profile if needed
                        authFlowManager.authViewModel.checkAuthState()
                        // Navigate to dashboard and clear login from backstack
                        navController.navigate(OpenRoutes.DASHBOARD) {
                            popUpTo(OpenRoutes.LOGIN) {
                                inclusive = true
                            }
                        }
                    }
                }
            LaunchedEffect(Unit) {
                launcher.launch(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build()
                )
            }
            // After successful sign-in, ensure a Firestore user profile exists
            val fbUser = FirebaseAuth.getInstance().currentUser
            if (fbUser != null) {
                val userDocRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(fbUser.uid)
                userDocRef.get().addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        val newUser = FirestoreUser(
                            userId = fbUser.uid,
                            email = fbUser.email.orEmpty(),
                            name = fbUser.displayName.orEmpty(),
                            role = RoleUtils.ROLE_REGULAR
                        )
                        userDocRef.set(newUser)
                    }
                }
            }
        }

        composable(OpenRoutes.DASHBOARD) {
            DashboardScreen(
                navController = navController,
                locationPermissionHandler = locationPermissionHandler,
                floodReportRepository = floodReportRepository,
                networkUtils = networkUtils,
                storageUtil = storageUtil,
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
    const val ADMIN = "admin"
    const val FLOOD_REPORT = "floodReport"
    const val FLOOD_REPORT_ID = "floodReport/{reportId}"
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
