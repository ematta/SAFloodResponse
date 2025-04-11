package edu.utap.ui.screens.flood

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.utap.ui.components.FloodReportItem
import edu.utap.ui.viewmodel.FloodReportViewModel

/**
 * Local flood reports list screen composable.
 *
 * Displays a list of nearby flood reports, with loading and error states.
 *
 * @param viewModel The [FloodReportViewModel] managing flood report data.
 * @param modifier Modifier to apply to the root container.
 */
/**
 * Stateless UI for displaying a list of local flood reports.
 * This function is preview-friendly and does not depend on ViewModel or runtime state.
 *
 * @param reports List of flood reports to display.
 * @param isLoading Whether the loading indicator should be shown.
 * @param error Error message to display, if any.
 * @param modifier Modifier to apply to the root container.
 */
@Composable
fun LocalFloodListScreenContent(
    reports: List<edu.utap.models.FloodReport>,
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator()
            }
            error != null -> {
                Text(
                    text = error ?: "Unknown error",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            reports.isEmpty() -> {
                Text(
                    text = "No floods reported nearby",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(reports) { report ->
                        FloodReportItem(report = report)
                    }
                }
            }
        }
    }
}

/**
 * Local flood reports list screen composable.
 *
 * Displays a list of nearby flood reports, with loading and error states.
 *
 * @param viewModel The [FloodReportViewModel] managing flood report data.
 * @param modifier Modifier to apply to the root container.
 */
@Composable
fun LocalFloodListScreen(viewModel: FloodReportViewModel, modifier: Modifier = Modifier) {
    val reports by viewModel.localFloodReports.collectAsState()
    val isLoading by viewModel.reportsLoading.collectAsState()
    val error by viewModel.reportsError.collectAsState()

    LocalFloodListScreenContent(
        reports = reports,
        isLoading = isLoading,
        error = error,
        modifier = modifier
    )
}

/**
 * Preview for LocalFloodListScreenContent.
 *
 * This preview uses sample data and does not require a ViewModel or runtime dependencies.
 * It demonstrates compliance with Jetpack Compose @Preview best practices:
 * - No ViewModel or DI dependencies
 * - Uses sample data
 * - Stateless and deterministic
 */
@androidx.compose.ui.tooling.preview.Preview(
    name = "Local Flood List - Sample Data",
    showBackground = true
)
@Composable
fun LocalFloodListScreenContentPreview() {
    // Sample data for preview
    val sampleReports = listOf(
        edu.utap.models.FloodReport(
            reportId = "1",
            userId = "user1",
            latitude = 30.2672,
            longitude = -97.7431,
            description = "Flooded street at 5th Ave",
            photoUrls = listOf(),
            status = "confirmed",
            severity = "high",
            waterDepthInches = 12.0,
            isRoadClosed = true
        ),
        edu.utap.models.FloodReport(
            reportId = "2",
            userId = "user2",
            latitude = 30.2673,
            longitude = -97.7432,
            description = "Minor flooding near park",
            photoUrls = listOf(),
            status = "pending",
            severity = "low",
            waterDepthInches = 2.0,
            isRoadClosed = false
        )
    )
    LocalFloodListScreenContent(
        reports = sampleReports,
        isLoading = false,
        error = null
    )
}
