package edu.utap.ui.screens.flood

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
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
@Composable
fun LocalFloodListScreen(
    viewModel: FloodReportViewModel,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.localFloodReports.collectAsState()
    val isLoading by viewModel.reportsLoading.collectAsState()
    val error by viewModel.reportsError.collectAsState()

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
