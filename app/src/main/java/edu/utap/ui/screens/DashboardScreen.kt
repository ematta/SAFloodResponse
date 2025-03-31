package edu.utap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import edu.utap.location.LocationPermissionHandler
import edu.utap.ui.viewmodel.FloodReportViewModel
import edu.utap.ui.viewmodel.WeatherViewModel

@Composable
fun DashboardScreen(
    navController: NavController,
    locationPermissionHandler: LocationPermissionHandler,
    weatherViewModel: WeatherViewModel = viewModel(),
    floodReportViewModel: FloodReportViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var isLocationPermissionGranted by remember { mutableStateOf(false) }
    val floodAlerts by weatherViewModel.floodAlerts.collectAsState()
    val isLoading by weatherViewModel.isLoading.collectAsState()
    val weatherError by weatherViewModel.error.collectAsState()
    
    // Flood reports state
    val floodReports by floodReportViewModel.observeAllReports().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        locationPermissionHandler.checkAndRequestLocationPermission(
            onGranted = { isLocationPermissionGranted = true },
            onDenied = { isLocationPermissionGranted = false }
        )
    }

    val sanAntonio = LatLng(29.4241, -98.4936) // San Antonio coordinates
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(sanAntonio, 12f)
    }

    // Fetch flood alerts when the screen is loaded
    LaunchedEffect(Unit) {
        weatherViewModel.fetchFloodAlerts(sanAntonio.latitude, sanAntonio.longitude)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Google Map - takes 60% of screen
            GoogleMap(
                modifier = Modifier.weight(0.6f),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = isLocationPermissionGranted)
        ) {
            // Add markers for flood alerts
            floodAlerts.forEach { alert ->
                Marker(
                    state = MarkerState(position = LatLng(alert.latitude, alert.longitude)),
                    title =
                    alert.title,
                    snippet = alert.description,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        when (alert.severity.lowercase()) {
                            "extreme" -> BitmapDescriptorFactory.HUE_RED
                            "severe" -> BitmapDescriptorFactory.HUE_ORANGE
                            "moderate" -> BitmapDescriptorFactory.HUE_YELLOW
                            else -> BitmapDescriptorFactory.HUE_BLUE
                        }
                    )
                )
            }
        }

        // Show loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
            )
        }

        // Show error message if any
        weatherError?.let { errorMessage ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Text(errorMessage)
            }
        }

        if (!isLocationPermissionGranted) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = {
                        locationPermissionHandler.checkAndRequestLocationPermission(
                            onGranted = { isLocationPermissionGranted = true },
                            onDenied = { isLocationPermissionGranted = false }
                        )
                    }) {
                        Text("Grant Permission")
                    }
                }
            ) {
                Text("Location permission is required to show your location on the map")
            }
        }

                // Flood Reports List - takes 40% of screen
                LazyColumn(modifier = Modifier.weight(0.4f)) {
                    items(floodReports) { report ->
                        FloodReportItem(report = report)
                    }
                }
            }

            // Bottom Navigation
            NavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home") },
                selected = true,
                onClick = { /* Already on home */ }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Menu, contentDescription = "Discussions") },
                label = { Text("Discussions") },
                selected = false,
                onClick = { navController.navigate("discussions") }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Warning, contentDescription = "Emergency") },
                label = { Text("Emergency") },
                selected = false,
                onClick = { navController.navigate("emergency") }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = false,
                onClick = { navController.navigate("profile") }
            )
        }

        // Floating Action Button for refreshing alerts
        FloatingActionButton(
            onClick = {
                weatherViewModel.fetchFloodAlerts(sanAntonio.latitude, sanAntonio.longitude)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 80.dp) // Add padding to place above navigation bar
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh Alerts")
        }
    }
}
