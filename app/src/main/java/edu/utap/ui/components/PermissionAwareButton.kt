package edu.utap.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.auth.model.AuthViewModel
import edu.utap.utils.RoleUtils

/**
 * A button that is only visible to users with sufficient permissions.
 *
 * This composable is useful for conditionally showing UI elements based on the user's role.
 * For example, admin-only features or volunteer-specific actions.
 *
 * @param text The text to display on the button
 * @param requiredRole The minimum role required to see the button
 * @param onClick The action to perform when the button is clicked
 * @param modifier Modifier for the button
 * @param enabled Whether the button is enabled
 */
@Composable
fun PermissionAwareButton(
    text: String,
    requiredRole: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    // Only render the button if the user has sufficient permissions
    if (currentUser != null && RoleUtils.hasPermission(currentUser!!.role, requiredRole)) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        ) {
            Text(text)
        }
    }
}
