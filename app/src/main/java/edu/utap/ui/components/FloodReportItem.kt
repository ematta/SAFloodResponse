package edu.utap.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.utap.flood.model.FloodReport
import edu.utap.ui.theme.Dimensions

/**
 * Displays a card summarizing a flood report.
 *
 * Shows the report's description, status, and location with an icon indicating severity.
 *
 * @param report The [FloodReport] to display.
 * @param modifier Modifier to apply to the card.
 */
@Composable
fun FloodReportItem(
    report: FloodReport,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimensions.spacing_8)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.spacing_16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Flood Report",
                tint = when (report.status) {
                    "confirmed" -> Color.Red
                    "pending" -> Color.Yellow
                    else -> Color.Gray
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.size(Dimensions.spacing_16))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.size(Dimensions.spacing_8))
                
                Text(
                    text = "Status: ${report.status}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Text(
                    text = "Location: (${"%.4f".format(report.latitude)}, ${"%.4f".format(report.longitude)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
