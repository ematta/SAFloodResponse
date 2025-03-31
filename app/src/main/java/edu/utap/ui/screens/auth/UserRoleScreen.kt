package edu.utap.ui.screens.auth

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
import edu.utap.auth.model.AuthViewModel
import edu.utap.db.UserEntity
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
                        UserRoleItem(user, authViewModel)
                    }
                }
            }
        },
        fallback = {
            // Show unauthorized message if user doesn't have admin role
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "You don't have permission to access this screen.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onNavigateBack) {
                    Text("Go Back")
                }
            }
        }
    )
}

@Composable
private fun UserRoleItem(user: UserEntity, authViewModel: AuthViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(user.role) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = user.name ?: "No Name",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = user.email ?: "No Email",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Role: ${selectedRole ?: "None"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Box {
                    Button(onClick = { expanded = true }) {
                        Text("Change Role")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(RoleUtils.ROLE_REGULAR) },
                            onClick = {
                                selectedRole = RoleUtils.ROLE_REGULAR
                                authViewModel.updateUserRole(user.userId, RoleUtils.ROLE_REGULAR)
                                expanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(RoleUtils.ROLE_ADMIN) },
                            onClick = {
                                selectedRole = RoleUtils.ROLE_ADMIN
                                authViewModel.updateUserRole(user.userId, RoleUtils.ROLE_ADMIN)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
