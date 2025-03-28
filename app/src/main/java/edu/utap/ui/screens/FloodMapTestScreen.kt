package edu.utap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*
import kotlin.random.Random
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import edu.utap.flood.FloodReport
import edu.utap.flood.FloodSeverity
import edu.utap.ui.viewmodel.FloodMapTestViewModel

@Composable
fun FloodMapTestScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: FloodMapTestViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Initial camera position centered on San Antonio, TX
    val sanAntonioPosition = LatLng(29.4241, -98.4936)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(sanAntonioPosition, 10f)
    }
    
    // Track if map properties should be shown
    var showMapProperties by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Flood Map Test Screen",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // Google Maps component
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                )
            ) {
                // Display markers for each flood report
                state.floodReports.forEach { report ->
                    MapMarker(
                        position = report.location,
                        title = "Flood: ${report.severity}",
                        snippet = report.description,
                        iconTint = getColorForSeverity(report.severity)
                    )
                    
                    // Show circle overlay to represent the flood area
                    Circle(
                        center = report.location,
                        radius = report.radius.toDouble(),
                        fillColor = report.severity.toColor().toComposeColor().copy(alpha = 0.3f),
                        strokeColor = report.severity.toColor().toComposeColor(),
                        strokeWidth = 2f
                    )
                }
            }
            
            // Display flood report count
            if (state.floodReports.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        text = "${state.floodReports.size} Flood Reports",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Test controls
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { generateFakeFloodReport(viewModel) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Generate Fake Flood Report")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        if (state.floodReports.isNotEmpty()) {
                            // Focus camera on latest flood report
                            val latest = state.floodReports.last()
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latest.location, 14f)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Focus Latest")
                }
                
                Button(
                    onClick = { viewModel.clearSimulation() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            }
        }
    }
}

@Composable
private fun MapMarker(
    position: LatLng,
    title: String,
    snippet: String,
    iconTint: Color = Color.Red
) {
    val markerState = rememberMarkerState(position = position)
    Marker(
        state = markerState,
        title = title,
        snippet = snippet,
        icon = BitmapDescriptorFactory.defaultMarker(
            when (iconTint) {
                Color.Green -> BitmapDescriptorFactory.HUE_GREEN
                Color.Cyan -> BitmapDescriptorFactory.HUE_CYAN
                Color.Yellow -> BitmapDescriptorFactory.HUE_YELLOW
                Color.Red -> BitmapDescriptorFactory.HUE_RED
                else -> BitmapDescriptorFactory.HUE_RED
            }
        )
    )
}

private fun getColorForSeverity(severity: FloodSeverity): Color {
    return when (severity) {
        FloodSeverity.LOW -> Color.Green
        FloodSeverity.MEDIUM -> Color.Cyan
        FloodSeverity.HIGH -> Color.Yellow
        FloodSeverity.EXTREME -> Color.Red
    }
}

private fun Long.toComposeColor(): Color {
    return Color(
        alpha = (this shr 24 and 0xff) / 255f,
        red = (this shr 16 and 0xff) / 255f,
        green = (this shr 8 and 0xff) / 255f,
        blue = (this and 0xff) / 255f
    )
}

private fun generateFakeFloodReport(viewModel: FloodMapTestViewModel) {
    // Generate a random location in San Antonio, TX area
    val sanAntonioLat = 29.4241
    val sanAntonioLng = -98.4936
    
    // Random offset within ~5km
    val latOffset = (Random.nextDouble() - 0.5) * 0.05
    val lngOffset = (Random.nextDouble() - 0.5) * 0.05
    
    val randomLocation = LatLng(
        sanAntonioLat + latOffset,
        sanAntonioLng + lngOffset
    )
    
    // Random severity
    val severities = FloodSeverity.values()
    val randomSeverity = severities[Random.nextInt(severities.size)]
    
    // Generate a description
    val descriptions = listOf(
        "Street flooding reported",
        "Water rising quickly",
        "Intersection flooded",
        "Flash flood in progress",
        "Creek overflowing"
    )
    val randomDescription = descriptions[Random.nextInt(descriptions.size)]
    
    // Create and add the flood report
    val report = FloodReport(
        location = randomLocation,
        severity = randomSeverity,
        description = randomDescription
    )
    
    viewModel.addFloodReport(report)
} 
