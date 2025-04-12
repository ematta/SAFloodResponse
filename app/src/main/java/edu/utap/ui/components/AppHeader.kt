package edu.utap.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import edu.utap.AuthenticatedRoutes
import edu.utap.R
import edu.utap.ui.navigation.OpenRoutes

/**
 * Top app bar composable for the Flood Response app.
 *
 * Displays the app logo and title, with an optional test screen button.
 *
 * @param onClickEvent Callback invoked when the test button is clicked.
 */
@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(navController: NavController) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_water_24),
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
            IconButton(onClick = {
                navController.navigate(OpenRoutes.LOGIN)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_login_24),
                    contentDescription = "Login",
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
