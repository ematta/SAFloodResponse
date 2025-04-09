package edu.utap.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.models.AuthViewModel
import edu.utap.models.FirestoreUser

/**
 * A composable that conditionally renders content based on the user's role permissions.
 *
 * This composable simplifies permission-based UI rendering by checking if the current user
 * has the required role permission before displaying the protected content.
 *
 * @param requiredRole The minimum role required to view the content
 * @param content The content to display if the user has permission
 * @param fallback Optional fallback content to display if the user doesn't have permission
 */
@Composable
fun PermissionCheck(
    requiredRole: String,
    authViewModel: AuthViewModel = viewModel(),
    content: @Composable () -> Unit,
    fallback: @Composable (() -> Unit)? = null
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    if (currentUser != null && hasRequiredPermission(currentUser!!, requiredRole)) {
        content()
    } else {
        fallback?.invoke()
    }
}

/**
 * Checks if a user entity has the required permissions.
 *
 * @param user The user entity to check
 * @param requiredRole The minimum role required
 * @return True if the user has sufficient permissions, false otherwise
 */
private fun hasRequiredPermission(user: FirestoreUser, requiredRole: String): Boolean {
    // TODO: Implement permission check based on FirebaseUser.
    // FirebaseUser does not have a 'role' property directly.
    // Need to fetch role from Firestore or use custom claims.
    // RoleUtils.hasPermission(user.role, requiredRole) // This line will break
    return false // Placeholder
}
