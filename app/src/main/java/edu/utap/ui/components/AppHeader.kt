package edu.utap.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.utap.R

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Top app bar composable for the Flood Response app.
 *
 * Displays the app logo and title, with an optional test screen button.
 *
 * @param onTestScreenClick Callback invoked when the test button is clicked.
 */
@Composable
fun AppHeader(onTestScreenClick: () -> Unit = {}) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_smiley_face),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SA Floods",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        actions = {
            // Test screen button (temporary)
            IconButton(onClick = onTestScreenClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_smiley_face),
                    contentDescription = "Test Flood Map",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}
