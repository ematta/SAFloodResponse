package edu.utap.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import edu.utap.AuthenticatedRoutes
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

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
