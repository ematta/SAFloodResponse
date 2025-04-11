package edu.utap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import edu.utap.models.FloodReport

// --- Stateless UI Composable ---

@Composable
fun DashboardContent(
    isLocationPermissionGranted: Boolean,
    isLoading: Boolean,
    allReports: List<FloodReport>,
    activeFloodReports: List<FloodReport>,
    onRequestLocationPermission: () -> Unit,
    onRefreshAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(29.4241, -98.4936), // Default to San Antonio
            12f
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // If permission granted, show map; else, show limited functionality message
            if (isLocationPermissionGranted) {
                // Google Map - takes 60% of screen
                GoogleMap(
                    modifier = Modifier.weight(0.6f),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = isLocationPermissionGranted)
                ) {
                    // Add markers for flood alerts
                    allReports.forEach { report ->
                        Marker(
                            state = MarkerState(position = LatLng(report.latitude, report.longitude)),
                            title = report.description,
                            snippet = "Severity: ${report.severity}",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                when (report.severity) {
                                    "low" -> BitmapDescriptorFactory.HUE_GREEN
                                    "medium" -> BitmapDescriptorFactory.HUE_YELLOW
                                    "high" -> BitmapDescriptorFactory.HUE_RED
                                    else -> BitmapDescriptorFactory.HUE_BLUE
                                }
                            )
                        )
                    }
                }
            } else {
                // Show clear, user-friendly message about limited functionality
                Card(
                    modifier = Modifier
                        .weight(0.6f)
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
            // Flood report list view
            LazyColumn(modifier = Modifier.weight(0.4f)) {
                items(activeFloodReports) { report ->
                    Text(
                        text = report.description,
                        modifier = Modifier.padding(8.dp)
                    )
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
 * @param weatherViewModel ViewModel for weather alerts.
 * @param floodReportRepository Repository for flood reports.
 * @param modifier Modifier to apply to the root container.
 */
@Composable
fun DashboardScreen(
    navController: androidx.navigation.NavController,
    locationPermissionHandler: edu.utap.utils.LocationPermissionHandler,
    weatherViewModel: edu.utap.ui.viewmodel.WeatherViewModel,
    floodReportRepository: edu.utap.repository.FloodReportRepositoryInterface,
    networkUtils: edu.utap.utils.NetworkUtilsInterface,
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

    // TODO: Replace the following with real data from ViewModels/Repositories
    val allReports = emptyList<edu.utap.models.FloodReport>()
    val activeFloodReports = emptyList<edu.utap.models.FloodReport>()
    val isLoading = false

    DashboardContent(
        isLocationPermissionGranted = isLocationPermissionGranted == true,
        isLoading = isLoading,
        allReports = allReports,
        activeFloodReports = activeFloodReports,
        onRequestLocationPermission = {
            locationPermissionHandler.checkAndRequestLocationPermission(
                onGranted = { isLocationPermissionGranted = true },
                onDenied = { isLocationPermissionGranted = false }
            )
        },
        onRefreshAlerts = { /* TODO: Implement refresh logic */ },
        modifier = modifier
    )
}

// --- Preview with Mock Data ---

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
    locale = "en"
)
@Composable
fun DashboardContentPreview() {
    val isInPreview = LocalInspectionMode.current

    val mockAllReports = if (isInPreview) {
        listOf(
            sampleFloodReport("1", 29.425, -98.49, "Minor street flooding", "low"),
            sampleFloodReport("2", 29.426, -98.491, "Major flooding near river", "high"),
            sampleFloodReport("3", 29.427, -98.492, "Moderate flooding downtown", "medium")
        )
    } else {
        emptyList()
    }

    val mockActiveReports = if (isInPreview) {
        listOf(
            sampleFloodReport("2", 29.426, -98.491, "Major flooding near river", "high"),
            sampleFloodReport("3", 29.427, -98.492, "Moderate flooding downtown", "medium")
        )
    } else {
        emptyList()
    }

    DashboardContent(
        isLocationPermissionGranted = false,
        isLoading = false,
        allReports = mockAllReports,
        activeFloodReports = mockActiveReports,
        onRequestLocationPermission = {},
        onRefreshAlerts = {},
        modifier = Modifier
    )
}
