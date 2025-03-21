package edu.utap.ui.screens.flood

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import edu.utap.models.FloodReport

/**
 * Card composable displaying a single flood report with confirm/deny buttons.
 *
 * @param report The [FloodReport] to display.
 * @param onConfirm Callback invoked when the report is confirmed.
 * @param onDeny Callback invoked when the report is denied.
 */
@Composable
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
