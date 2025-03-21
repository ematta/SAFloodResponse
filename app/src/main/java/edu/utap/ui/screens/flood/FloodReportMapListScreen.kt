package edu.utap.ui.screens.flood

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import edu.utap.models.FloodReport
import edu.utap.ui.navigation.AuthenticatedRoutes
import edu.utap.ui.navigation.OpenRoutes
import edu.utap.ui.viewmodel.FloodReportViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun FloodReportMapListScreen(viewModel: FloodReportViewModel, navController: NavHostController) {
    val floodReports by viewModel.activeFloodReports.collectAsState()
    var selectedReportId by remember { mutableStateOf<String?>(null) }
    val selectedReport = floodReports.find { it.reportId == selectedReportId }
    var isListCollapsed by remember { mutableStateOf(false) }

    // Always start map centered on San Antonio, TX
    val sanAntonioLatLng = LatLng(29.4241, -98.4936)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(sanAntonioLatLng, 10f) // Adjusted initial zoom level
    }

    // Animate camera when selection changes
    LaunchedEffect(selectedReport) {
        selectedReport?.let {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(it.latitude, it.longitude),
                    12f // Adjusted focused zoom level
                ),
                durationMs = 800
            )
        }
    }

    Row(Modifier.fillMaxSize()) {
        // Collapsible flood list panel
        Box(
            modifier = Modifier
                .weight(0.4f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Flood Reports",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f).padding(8.dp)
                    )
                    IconButton(onClick = { isListCollapsed = true }) {
                        Icons.Default.KeyboardArrowLeft
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(8.dp)
                ) {
                    items(floodReports) { report ->
                        val isSelected = report.reportId == selectedReportId
                        if (report.status.lowercase() == "active") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.15f
                                        ) else Color.Transparent
                                    )
                                    .clickable { selectedReportId = report.reportId },
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        report.description,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        "Lat: %.4f, Lng: %.4f".format(
                                            report.latitude,
                                            report.longitude
                                        ), style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Status: ${report.status}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // Map and details column
        Column(Modifier.weight(if (isListCollapsed) 1f else 0.6f).fillMaxHeight()) {
            // Map
            Box(Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    floodReports.forEach { report ->
                        Marker(
                            state = MarkerState(com.google.android.gms.maps.model.LatLng(report.latitude, report.longitude)),
                            title = report.description,
                            snippet = "Status: ${report.status}",
                            onClick = {
                                selectedReportId = report.reportId
                                false
                            },
                            icon = if (report.reportId == selectedReportId) BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE) else null
                        )
                    }
                }
            }
            // Flood details panel
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp, max = 180.dp),
                onClick = {
                    selectedReport?.let { report ->
                        navController.navigate(
                            AuthenticatedRoutes.FLOOD_REPORT_ID.replace(
                                oldValue = "{reportId}",
                                newValue = report.reportId
                            )
                        )
                    }
                }
            ) {
                if (selectedReport != null) {
                    Column(Modifier.padding(16.dp)) {
                        // Wrap description in a link composable
                        Text(selectedReport.description, style = MaterialTheme.typography.titleMedium)
                        Text("Description:", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("Location: %.4f, %.4f".format(selectedReport.latitude, selectedReport.longitude), style = MaterialTheme.typography.bodySmall)
                        Text("Severity: ${selectedReport.severity}", style = MaterialTheme.typography.bodySmall)
                        Text("Status: ${selectedReport.status}", style = MaterialTheme.typography.bodySmall)
                        Text("Water Depth: ${selectedReport.waterDepthInches} inches", style = MaterialTheme.typography.bodySmall)
                        Text("Road Closed: ${if (selectedReport.isRoadClosed) "Yes" else "No"}", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a flood report to see details.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }
        }
    }
}
