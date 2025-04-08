package edu.utap.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import edu.utap.AuthenticatedRoutes
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

/**
 * Bottom navigation bar composable for the Flood Response app.
 *
 * Provides navigation between Home, Report, Discussions, and Profile screens.
 *
 * @param navController The navigation controller used to navigate between screens.
 * @param currentRoute The current navigation route to highlight the selected item.
 */
@Composable
fun AppBottomNavigation(navController: NavController, currentRoute: String) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == AuthenticatedRoutes.DASHBOARD,
            onClick = {
                if (currentRoute != AuthenticatedRoutes.DASHBOARD) {
                    navController.navigate(AuthenticatedRoutes.DASHBOARD) {
                        popUpTo(AuthenticatedRoutes.DASHBOARD)
                    }
                }
            },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )

        NavigationBarItem(
            selected = currentRoute == AuthenticatedRoutes.FLOOD_REPORT,
            onClick = {
                if (currentRoute != AuthenticatedRoutes.FLOOD_REPORT) {
                    navController.navigate(AuthenticatedRoutes.FLOOD_REPORT) {
                        popUpTo(AuthenticatedRoutes.FLOOD_REPORT)
                    }
                }
            },
            icon = { Icon(Icons.Filled.Warning, contentDescription = "Report") },
            label = { Text("Report") }
        )

        NavigationBarItem(
            selected = currentRoute == AuthenticatedRoutes.DISCUSSIONS,
            onClick = {
                if (currentRoute != AuthenticatedRoutes.DISCUSSIONS) {
                    navController.navigate(AuthenticatedRoutes.DISCUSSIONS) {
                        popUpTo(AuthenticatedRoutes.DISCUSSIONS)
                    }
                }
            },
            icon = { Icon(Icons.Filled.Menu, contentDescription = "Discussions") },
            label = { Text("Discussions") }
        )

        NavigationBarItem(
            selected = currentRoute == AuthenticatedRoutes.PROFILE,
            onClick = {
                if (currentRoute != AuthenticatedRoutes.PROFILE) {
                    navController.navigate(AuthenticatedRoutes.PROFILE) {
                        popUpTo(AuthenticatedRoutes.PROFILE)
                    }
                }
            },
            icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}
