package edu.utap.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.utap.models.FloodReport
import edu.utap.ui.viewmodel.FloodReportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: FloodReportViewModel,
    onNavigateBack: () -> Unit
) {
    val reports by viewModel.allReports.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reports) { report ->
                ReportStatusItem(
                    report = report,
                    onStatusChange = { newStatus ->
                        viewModel.setReportStatus(report.reportId, newStatus)
                    }
                )
            }
        }
    }
}

@Composable
private fun ReportStatusItem(
    report: FloodReport,
    onStatusChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Current status: ${report.status}",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statuses = listOf("pending", "active", "inactive")
                statuses.forEach { status ->
                    Button(
                        onClick = { onStatusChange(status) },
                        enabled = report.status != status
                    ) {
                        Text(status.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }
}
