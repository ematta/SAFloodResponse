package edu.utap.flood.ui

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
import edu.utap.flood.FloodReportViewModel
import edu.utap.flood.ReportState
import edu.utap.flood.model.FloodReport
import edu.utap.flood.utils.LocationUtils

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
fun FloodReportScreen(
    viewModel: FloodReportViewModel,
    onNavigateToMap: () -> Unit
) {
    val context = LocalContext.current
    val locationUtils = remember { LocationUtils(context) }
    
    // Collect state
    val reportState by viewModel.reportState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val selectedPhotos by viewModel.selectedPhotos.collectAsState()
    val description by viewModel.description.collectAsState()
    val isManualLocation by viewModel.isManualLocation.collectAsState()
    val manualLatitude by viewModel.manualLatitude.collectAsState()
    val manualLongitude by viewModel.manualLongitude.collectAsState()

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
                        
                        // Current Location Display
                        currentLocation?.let { location ->
                            Text(
                                text = "Current Location: ${location.latitude}, ${location.longitude}",
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
                                onCheckedChange = { viewModel.setManualLocation(it) }
                            )
                        }

                        // Manual Location Input
                        if (isManualLocation) {
                            OutlinedTextField(
                                value = manualLatitude.toString(),
                                onValueChange = { 
                                    it.toDoubleOrNull()?.let { lat ->
                                        viewModel.updateManualLocation(lat, manualLongitude)
                                    }
                                },
                                label = { Text("Latitude") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = manualLongitude.toString(),
                                onValueChange = { 
                                    it.toDoubleOrNull()?.let { lon ->
                                        viewModel.updateManualLocation(manualLatitude, lon)
                                    }
                                },
                                label = { Text("Longitude") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
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
                            onValueChange = { viewModel.updateDescription(it) },
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
                            onClick = { /* TODO: Implement photo picker */ },
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
                    onClick = { viewModel.submitReport() },
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
fun FloodReportItem(
    report: FloodReport,
    onConfirm: () -> Unit,
    onDeny: () -> Unit
) {
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
