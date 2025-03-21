package edu.utap.ui.screens.dashboard

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import edu.utap.di.FloodViewModelFactory
import edu.utap.repository.FloodReportRepository
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import edu.utap.models.FloodReport
import edu.utap.repository.FloodReportRepositoryInterface
import edu.utap.ui.screens.flood.FloodReportMapListScreen
import edu.utap.ui.viewmodel.FloodReportViewModel
import edu.utap.utils.FirebaseStorageUtilInterface
import edu.utap.utils.LocationPermissionHandler
import edu.utap.utils.NetworkUtilsInterface

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    isLocationPermissionGranted: Boolean,
    isLoading: Boolean,
    onRequestLocationPermission: () -> Unit,
    onRefreshAlerts: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(29.4241, -98.4936), // Default to San Antonio
            12f
        )
    }

    val floodReportViewModel: FloodReportViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.titleMedium // Use smaller text style
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(),
                modifier = Modifier.height(48.dp) // Reduce height
            )
        },
    ) { innerPadding ->
        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Ensure content is below the top bar
            ) {
                if (isLocationPermissionGranted) {
                    // Combined map and list with selection and sync
                    FloodReportMapListScreen(
                        viewModel = floodReportViewModel,
                        navController = navController as NavHostController
                    )
                } else {
                    // Show clear, user-friendly message about limited functionality
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Location access denied",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Some features are limited. Grant location permission to view the map and see your position relative to flood alerts.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRequestLocationPermission) {
                                Text("Grant Location Permission")
                            }
                        }
                    }
                }
                // Show loading indicator
                if (isLoading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    }
                }
                if (!isLocationPermissionGranted) {
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        action = {
                            TextButton(onClick = onRequestLocationPermission) {
                                Text("Grant Permission")
                            }
                        }
                    ) {
                        Text("Location permission is required to show your location on the map")
                    }
                }
            }
            // Floating Action Button for refreshing alerts
            FloatingActionButton(
                onClick = onRefreshAlerts,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 80.dp) // Add padding to place above navigation bar
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Alerts")
            }
        }
    }
}

// --- Original DashboardScreen, now delegates to DashboardContent ---

/**
 * Dashboard screen composable displaying:
 * - Google Map with flood alerts and user reports
 * - List of active flood reports
 * - Weather alerts and loading/error states
 * - Location permission handling
 * - Refresh button and bottom navigation
 *
 * @param navController Navigation controller for navigating between screens.
 * @param locationPermissionHandler Handler for location permissions.
 * @param floodReportRepository Repository for flood reports.
 * @param modifier Modifier to apply to the root container.
 */
@Composable
fun DashboardScreen(
    navController: NavController,
    locationPermissionHandler: LocationPermissionHandler,
    floodReportRepository: FloodReportRepositoryInterface,
    networkUtils: NetworkUtilsInterface,
    storageUtil: FirebaseStorageUtilInterface,
    modifier: Modifier = Modifier
) {
    // Permission state: null = not checked yet, true = granted, false = denied
    var isLocationPermissionGranted by remember { mutableStateOf<Boolean?>(null) }
    var permissionRequested by remember { mutableStateOf(false) }

    // Prompt for location permission immediately on entry
    LaunchedEffect(Unit) {
        if (!permissionRequested) {
            permissionRequested = true
            locationPermissionHandler.checkAndRequestLocationPermission(
                onGranted = { isLocationPermissionGranted = true },
                onDenied = { isLocationPermissionGranted = false }
            )
        }
    }

    // Obtain FloodReportViewModel instance
    val context = LocalContext.current
    val floodReportViewModel: FloodReportViewModel = viewModel(
        factory = FloodViewModelFactory(
            context = context,
            floodReportRepository = floodReportRepository as FloodReportRepository,
            networkUtils = networkUtils,
            storageUtil = storageUtil
        )
    )

    // Fetch active flood reports on load
    LaunchedEffect(Unit) {
        floodReportViewModel.refreshActiveFloodReports()
    }

    val activeFloodReports by floodReportViewModel.activeFloodReports.collectAsState()
    val isLoading = false

    DashboardContent(
        isLocationPermissionGranted = isLocationPermissionGranted == true,
        isLoading = isLoading,
        onRequestLocationPermission = {
            locationPermissionHandler.checkAndRequestLocationPermission(
                onGranted = { isLocationPermissionGranted = true },
                onDenied = { isLocationPermissionGranted = false }
            )
        },
        onRefreshAlerts = {
            floodReportViewModel.refreshActiveFloodReports()
        },
        modifier = modifier,
        navController = navController
    )
}

private fun sampleFloodReport(
    id: String,
    lat: Double,
    lng: Double,
    desc: String,
    severity: String
) = FloodReport(
    reportId = id,
    userId = "user$id",
    latitude = lat,
    longitude = lng,
    description = desc,
    severity = severity
)

@Preview(
    name = "DashboardContent Preview - Light",
    showBackground = true,
    device = "id:pixel_5",
    locale = "en",
)
@Composable
fun DashboardContentPreview() {
    val isInPreview = LocalInspectionMode.current

    DashboardContent(
        isLocationPermissionGranted = false,
        isLoading = false,
        onRequestLocationPermission = {},
        onRefreshAlerts = {},
        modifier = Modifier,
        navController = rememberNavController()
    )
}
