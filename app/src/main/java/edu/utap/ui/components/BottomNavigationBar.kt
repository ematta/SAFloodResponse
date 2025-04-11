package edu.utap.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import edu.utap.AuthenticatedRoutes
import edu.utap.R
import edu.utap.ui.navigation.OpenRoutes

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar (
        contentColor = Color.Black,
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.baseline_dashboard_24), contentDescription = null) },
            label = { Text(stringResource(id = R.string.dashboard)) },
            selected = false, // Update based on navigation state
            onClick = {
                navController.navigate(OpenRoutes.DASHBOARD)
            }
        )

        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.baseline_tag_faces_24), contentDescription = null) },
            label = { Text(stringResource(id = R.string.profile)) },
            selected = false, // Update based on navigation state
            onClick = {
                navController.navigate(AuthenticatedRoutes.PROFILE)
            }
        )

        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.baseline_add_24), contentDescription = null) },
            label = { Text(stringResource(id = R.string.report)) },
            selected = false, // Update based on navigation state
            onClick = {
                navController.navigate(AuthenticatedRoutes.FLOOD_REPORT)
            }
        )

        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.baseline_chat_24), contentDescription = null) },
            label = { Text(stringResource(id = R.string.discussion)) },
            selected = false, // Update based on navigation state
            onClick = {
                navController.navigate(AuthenticatedRoutes.DISCUSSIONS)
            }
        )
    }
}

@Preview(
    showBackground = true
)
@Composable
fun BottomNavigationBarPreview() {
    BottomNavigationBar(navController = NavController(LocalContext.current))
}
