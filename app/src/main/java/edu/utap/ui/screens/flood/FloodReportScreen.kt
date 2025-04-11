package edu.utap.ui.screens.flood

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import edu.utap.models.FloodReport
import edu.utap.ui.viewmodel.FloodReportViewModel
import edu.utap.ui.viewmodel.ReportState
import edu.utap.utils.LocationUtils
import androidx.compose.ui.tooling.preview.Preview

/**
 * Composable function that displays the flood reporting screen.
 *
 * This screen allows users to:
 * - Submit new flood reports
 * - Add photos to reports
 * - Set manual location
 * - View nearby reports
 * - Confirm or deny reports
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Flood report submission screen composable.
 *
 * Allows users to:
 * - Submit new flood reports with description, photos, and location
 * - Toggle manual location input
 * - View and confirm/deny nearby reports
 *
 * @param viewModel The [FloodReportViewModel] managing report data.
 * @param onNavigateToMap Callback invoked to navigate to the map screen.
 */
fun FloodReportScreen(viewModel: FloodReportViewModel, onNavigateToMap: () -> Unit) {
    val context = LocalContext.current
    val locationUtils = remember { LocationUtils(context) }

    // Collect state
    val reportState by viewModel.reportState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val selectedPhotos by viewModel.selectedPhotos.collectAsState()
    val description by viewModel.description.collectAsState()
    val isManualLocation by viewModel.isManualLocation.collectAsState()
    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()

    FloodReportScreenContent(
        reportState = reportState,
        currentLocation = currentLocation,
        selectedPhotos = selectedPhotos,
        description = description,
        isManualLocation = isManualLocation,
        latitude = latitude,
        longitude = longitude,
        onNavigateToMap = onNavigateToMap,
        onSetManualLocation = { viewModel.setManualLocation(it) },
        onUpdateLatitude = { viewModel.updateLatitude(it) },
        onUpdateLongitude = { viewModel.updateLongitude(it) },
        onUpdateDescription = { viewModel.updateDescription(it) },
        onAddPhoto = { viewModel.addPhoto(it) },
        onSubmitReport = { viewModel.submitReport() }
    )
}

/**
 * Stateless UI content for the Flood Report screen, suitable for previews.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloodReportScreenContent(
    reportState: ReportState,
    currentLocation: Location?,
    selectedPhotos: List<String>,
    description: String,
    isManualLocation: Boolean,
    latitude: Double,
    longitude: Double,
    onNavigateToMap: () -> Unit,
    onSetManualLocation: (Boolean) -> Unit,
    onUpdateLatitude: (Double) -> Unit,
    onUpdateLongitude: (Double) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onAddPhoto: (String) -> Unit,
    onSubmitReport: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Flood") },
                actions = {
                    IconButton(onClick = onNavigateToMap) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "View Map"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Location Section
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.titleMedium
                        )

                        fun loc(location: Location?) =
                            "Current Location: ${location?.latitude}, ${location?.longitude}"
                        // Current Location Display
                        currentLocation?.let { location ->
                            Text(
                                text = loc(location),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Manual Location Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Manual Location")
                            Switch(
                                checked = isManualLocation,
                                onCheckedChange = onSetManualLocation
                            )
                        }

                        OutlinedTextField(
                            value = latitude.toString(),
                            onValueChange = {
                                it.toDoubleOrNull()?.let { lat ->
                                    onUpdateLatitude(lat)
                                } ?: onUpdateLatitude(0.0)
                            },
                            label = { Text("Latitude") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = longitude.toString(),
                            onValueChange = {
                                it.toDoubleOrNull()?.let { lon ->
                                    onUpdateLongitude(lon)
                                } ?: onUpdateLongitude(0.0)
                            },
                            label = { Text("Longitude") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Description Section
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = onUpdateDescription,
                            label = { Text("Describe the flood") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            }

            // Photos Section
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Photos",
                            style = MaterialTheme.typography.titleMedium
                        )
                        // Photo Grid
                        LazyColumn {
                            items(selectedPhotos) { photoUrl ->
                                // Photo Item
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Flood photo",
                                    modifier = Modifier.size(100.dp)
                                )
                            }
                        }
                        Button(
                            onClick = { onAddPhoto("https://example.com/photo.jpg") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Photo")
                        }
                    }
                }
            }

            // Submit Button
            item {
                Button(
                    onClick = onSubmitReport,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = reportState !is ReportState.Loading
                ) {
                    when (reportState) {
                        is ReportState.Loading -> CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        else -> Text("Submit Report")
                    }
                }
            }

            // Error Message
            if (reportState is ReportState.Error) {
                item {
                    Text(
                        text = (reportState as ReportState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * Composable function that displays a flood report item.
 *
 * @param report The flood report to display
 * @param onConfirm Callback for when the report is confirmed
 * @param onDeny Callback for when the report is denied
 */
@Composable
/**
 * Card composable displaying a single flood report with confirm/deny buttons.
 *
 * @param report The [FloodReport] to display.
 * @param onConfirm Callback invoked when the report is confirmed.
 * @param onDeny Callback invoked when the report is denied.
 */
fun FloodReportItem(report: FloodReport, onConfirm: () -> Unit, onDeny: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Location: ${report.latitude}, ${report.longitude}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Severity: ${report.severity.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Water Depth: ${report.waterDepthInches} inches",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Road Status: ${if (report.isRoadClosed) "Closed" else "Open"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium
            )

            // Photo Grid
            LazyColumn {
                items(report.photoUrls) { photoUrl ->
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Flood photo",
                        modifier = Modifier.size(100.dp)
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Confirm (${report.confirmedCount})")
                }
                Button(
                    onClick = onDeny,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Deny (${report.deniedCount})")
                }
            }
        }
    }
}

// --- Previews and Sample Data for Jetpack Compose Tooling ---
// These previews follow official best practices for @Preview usage.

@Preview(showBackground = true, name = "Flood Report Item")
@Composable
fun FloodReportItemPreview() {
    val sampleReport = FloodReport(
        latitude = 30.2672,
        longitude = -97.7431,
        severity = "moderate",
        waterDepthInches = 12.0,
        isRoadClosed = true,
        description = "Sample flood report for preview. Water is rising quickly.",
        photoUrls = listOf("https://via.placeholder.com/100"),
        confirmedCount = 3,
        deniedCount = 1
    )
    FloodReportItem(
        report = sampleReport,
        onConfirm = {},
        onDeny = {}
    )
}

