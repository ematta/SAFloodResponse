package edu.utap.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import edu.utap.auth.model.AuthViewModelInterface
import edu.utap.utils.ValidationUtils

/**
 * Forgot password screen composable.
 *
 * Allows users to request a password reset email.
 * Displays success and error messages, and a link back to login.
 *
 * @param viewModel The authentication ViewModel interface.
 * @param onNavigateToLogin Callback invoked to navigate back to the login screen.
 */
@Composable
fun ForgotPasswordScreen(viewModel: AuthViewModelInterface, onNavigateToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Reset Password",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (isSuccess) {
            // Success message
            Text(
                text = "Password reset email sent. Please check your inbox.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Return to Login")
            }
        } else {
            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null // Clear error when user types
                },
                label = { Text("Email") },
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            // Reset button
            Button(
                onClick = {
                    // Validate email
                    val isEmailValid = ValidationUtils.isValidEmail(email)

                    if (!isEmailValid) {
                        emailError = "Please enter a valid email address"
                        return@Button
                    }

                    // If email is valid, attempt password reset
                    isLoading = true
                    viewModel.resetPassword(email) { success, message ->
                        isLoading = false
                        if (success) {
                            isSuccess = true
                        } else {
                            errorMessage = message ?: "Password reset failed"
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Reset Password")
                }
            }

            // Error message
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to login link
            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Back to Login")
            }
        }
    }
}
