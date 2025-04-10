package edu.utap.ui.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.ui.viewmodel.AuthViewModel
import edu.utap.models.FirestoreUser
import edu.utap.utils.PermissionCheck
import edu.utap.utils.RoleUtils

/**
 * Screen for administrators to manage user roles in the application.
 *
 * This screen is protected by permission checks to ensure only admin users can access it.
 * It displays a list of all users and allows changing their roles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRoleScreen(authViewModel: AuthViewModel = viewModel(), onNavigateBack: () -> Unit) {
    // Get the list of all users from the repository
    val users by authViewModel.observeAllUsers().collectAsStateWithLifecycle(emptyList())

    // Permissions check to ensure only admins can access this screen
    PermissionCheck(
        requiredRole = RoleUtils.ROLE_ADMIN,
        authViewModel = authViewModel,
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("User Role Management") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                // Back icon
                            }
                        }
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(users) { user ->
                        UserRoleItem(
                            user = user,
                            onRoleChange = { userId, newRole ->
                                authViewModel.updateUserRole(userId, newRole)
                            }
                        )
                    }
                }
            }
        },
        fallback = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You don't have permission to access this page",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onNavigateBack) {
                    Text("Go Back")
                }
            }
        }
    )
}

/**
 * Composable for a single user item in the role management list.
 *
 * @param user The user entity to display
 * @param onRoleChange Callback for when the user's role is changed
 */
@Composable
fun UserRoleItem(user: FirestoreUser, onRoleChange: (String, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val roles = RoleUtils.getAllRoles()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Role: ",
                    style = MaterialTheme.typography.bodyMedium
                )

                Box {
                    Button(
                        onClick = { expanded = true }
                    ) {
                        Text(user.role)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role) },
                                onClick = {
                                    onRoleChange(user.userId, role)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = RoleUtils.roleDescriptions[user.role] ?: "",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
