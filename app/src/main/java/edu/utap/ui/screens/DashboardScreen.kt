package edu.utap.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import edu.utap.location.LocationPermissionHandler

@Composable
fun DashboardScreen(
    navController: NavController,
    locationPermissionHandler: LocationPermissionHandler,
    modifier: Modifier = Modifier
) {
    var isLocationPermissionGranted by remember { mutableStateOf(false) }

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

    Box(modifier = modifier.fillMaxSize()) {
        // Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = isLocationPermissionGranted)
        )

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

        // Floating Action Button for adding markers/reports
        FloatingActionButton(
            onClick = { /* Handle adding new markers/reports */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 80.dp) // Add padding to place above navigation bar
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Report")
        }
    }
}