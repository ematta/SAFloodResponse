package edu.utap.ui.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
fun UserRoleScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val inPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    val users: List<FirestoreUser> =
        if (inPreview) {
            // Provide mock users for preview mode
            listOf(
                FirestoreUser(
                    userId = "1",
                    name = "Alice Admin",
                    email = "alice@admin.com",
                    role = RoleUtils.ROLE_ADMIN
                ),
                FirestoreUser(
                    userId = "2",
                    name = "Bob Volunteer",
                    email = "bob@volunteer.com",
                    role = RoleUtils.ROLE_VOLUNTEER
                ),
                FirestoreUser(
                    userId = "3",
                    name = "Carol Regular",
                    email = "carol@user.com",
                    role = RoleUtils.ROLE_REGULAR
                )
            )
        } else {
            // Get the list of all users from the repository
            val usersState by authViewModel.observeAllUsers().collectAsStateWithLifecycle(emptyList())
            usersState
        }

    // Permissions check to ensure only admins can access this screen
    PermissionCheck(
        requiredRole = RoleUtils.ROLE_ADMIN,
        authViewModel = authViewModel,
        content = {
            UserRoleScreenContent(
                users = users,
                onNavigateBack = onNavigateBack,
                onRoleChange = { userId, newRole ->
                    authViewModel.updateUserRole(userId, newRole)
                }
            )
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
 * Stateless UI content for the user role management screen.
 * Accepts a list of users and navigation/role change lambdas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRoleScreenContent(
    users: List<FirestoreUser>,
    onNavigateBack: () -> Unit,
    onRoleChange: (String, String) -> Unit
) {
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
                    onRoleChange = onRoleChange
                )
            }
        }
    }
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

/**
 * PreviewParameterProvider for FirestoreUser to enable parameterized previews.
 */
class FirestoreUserPreviewProvider : androidx.compose.ui.tooling.preview.PreviewParameterProvider<FirestoreUser> {
    override val values: Sequence<FirestoreUser> = sequenceOf(
        FirestoreUser(
            userId = "1",
            name = "Alice Admin",
            email = "alice@admin.com",
            role = RoleUtils.ROLE_ADMIN
        ),
        FirestoreUser(
            userId = "2",
            name = "Bob Volunteer",
            email = "bob@volunteer.com",
            role = RoleUtils.ROLE_VOLUNTEER
        ),
        FirestoreUser(
            userId = "3",
            name = "Carol Regular",
            email = "carol@user.com",
            role = RoleUtils.ROLE_REGULAR
        )
    )
}

/**
 * Preview for UserRoleScreenContent with mock users and a no-op navigation lambda.
 */
@Preview(
    name = "UserRoleScreenContent",
    showBackground = true
)
@Composable
fun PreviewUserRoleScreenContent() {
    val users = listOf(
        FirestoreUser(
            userId = "1",
            name = "Alice Admin",
            email = "alice@admin.com",
            role = RoleUtils.ROLE_ADMIN
        ),
        FirestoreUser(
            userId = "2",
            name = "Bob Volunteer",
            email = "bob@volunteer.com",
            role = RoleUtils.ROLE_VOLUNTEER
        ),
        FirestoreUser(
            userId = "3",
            name = "Carol Regular",
            email = "carol@user.com",
            role = RoleUtils.ROLE_REGULAR
        )
    )
    UserRoleScreenContent(
        users = users,
        onNavigateBack = {},
        onRoleChange = { _, _ -> }
    )
}

/**
 * Preview for UserRoleItem with a sample user and a no-op onRoleChange lambda.
 */
@Preview(
    name = "UserRoleItem - Admin",
    showBackground = true
)
@Composable
fun PreviewUserRoleItemAdmin() {
    UserRoleItem(
        user = FirestoreUser(
            userId = "1",
            name = "Alice Admin",
            email = "alice@admin.com",
            role = RoleUtils.ROLE_ADMIN
        ),
        onRoleChange = { _, _ -> }
    )
}

/**
 * Parameterized preview for UserRoleItem using FirestoreUserPreviewProvider.
 */
@Preview(
    name = "UserRoleItem - Parameterized",
    showBackground = true
)
@Composable
fun PreviewUserRoleItemParameterized(
    @androidx.compose.ui.tooling.preview.PreviewParameter(FirestoreUserPreviewProvider::class) user: FirestoreUser
) {
    UserRoleItem(
        user = user,
        onRoleChange = { _, _ -> }
    )
}
