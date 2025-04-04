package edu.utap.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import edu.utap.flood.model.FloodReport
import edu.utap.flood.repository.FloodReportRepositoryInterface
import edu.utap.location.LocationPermissionHandler
import edu.utap.ui.components.AppBottomNavigation
import edu.utap.ui.components.FloodReportItem
import edu.utap.ui.viewmodel.WeatherViewModel

private const val TAG = "DashboardScreen"

@Composable
fun DashboardScreen(
    navController: NavController,
    locationPermissionHandler: LocationPermissionHandler,
    weatherViewModel: WeatherViewModel = viewModel(),
    floodReportRepository: FloodReportRepositoryInterface,
    modifier: Modifier = Modifier
) {
    var isLocationPermissionGranted by remember { mutableStateOf(false) }
    val floodAlerts by weatherViewModel.floodAlerts.collectAsState()
    val isLoading by weatherViewModel.isLoading.collectAsState()
    val weatherError by weatherViewModel.error.collectAsState()

    // Flood reports state
    val sanAntonio = LatLng(29.4241, -98.4936) // San Antonio coordinates
    Log.d(TAG, "Fetching reports for location: $sanAntonio with 50 mile radius")
    val floodReports by remember {
        floodReportRepository.getReportsInRadius(
            sanAntonio.latitude, sanAntonio.longitude, 50.0 // 50 mile radius
        ).also { flow ->
            Log.d(TAG, "Flow created for reports")
        }
    }.collectAsState(initial = emptyList<FloodReport>().also {
        Log.d(TAG, "Initial empty state set for reports")
    }).also { state ->
        LaunchedEffect(state.value) {
            Log.d(TAG, "Reports updated: ${state.value.size} reports")
            state.value.forEach {
                Log.d(TAG, "Report: ${it.reportId} at (${it.latitude}, ${it.longitude})")
            }
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionHandler.checkAndRequestLocationPermission(
            onGranted = {
                isLocationPermissionGranted = true
            },
            onDenied = { isLocationPermissionGranted = false })
    }

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
                        title = alert.title,
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
                Box(
                    contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
            }

            // Show error message if any
            weatherError?.let { errorMessage ->
                Box(
                    contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxWidth()
                ) {
                    Snackbar(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(errorMessage)
                    }
                }
            }

            if (!isLocationPermissionGranted) {
                Snackbar(
                    modifier = Modifier.padding(16.dp), action = {
                        TextButton(onClick = {
                            locationPermissionHandler.checkAndRequestLocationPermission(
                                onGranted = {
                                    isLocationPermissionGranted = true
                                },
                                onDenied = { isLocationPermissionGranted = false })
                        }) {
                            Text("Grant Permission")
                        }
                    }) {
                    Text("Location permission is required to show your location on the map")
                }
            }

            // Flood Reports List - takes 40% of screen
            Box(modifier = Modifier.weight(0.4f)) {
                if (floodReports.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
                    ) {
                        Text("No flood reports found")
                    }
                } else {
                    LazyColumn {
                        items(floodReports) { report ->
                            FloodReportItem(report = report)
                        }
                    }
                }
            }
        }

        AppBottomNavigation(navController, "dashboard")

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
