package edu.utap.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.auth.model.AuthViewModel
import edu.utap.auth.model.AuthViewModelInterface
import edu.utap.utils.ValidationUtils

/**
 * Composable screen for user login with email and password.
 *
 * This screen provides input fields for email and password, performs validation,
 * displays error messages, and triggers authentication via the provided ViewModel.
 * It also provides navigation options to registration and password reset screens.
 *
 * @param authViewModel The authentication ViewModel interface used to perform login.
 * @param onNavigateToRegister Callback invoked to navigate to the registration screen.
 * @param onNavigateToForgotPassword Callback invoked to navigate to the forgot password screen.
 * @param onLoginSuccess Callback invoked when login is successful.
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModelInterface = viewModel<AuthViewModel>(),
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Track validation errors for each field
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Check if user is already authenticated
    if (authState is AuthState.Idle.Authenticated) {
        onLoginSuccess()
        return
    }

    // Update error message when auth state contains an error
    if (authState is AuthState.Error) {
        errorMessage = when (authState) {
            is AuthState.Error.Generic ->
                (authState as AuthState.Error.Generic).message
            is AuthState.Error.Network ->
                (authState as AuthState.Error.Network).message
            is AuthState.Error.Authentication ->
                (authState as AuthState.Error.Authentication).message
            is AuthState.Error.Validation ->
                (authState as AuthState.Error.Validation).message
            else -> "Unknown error occurred"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .padding(horizontal = 16.dp)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                // Clear error when user starts typing
                emailError = null
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = emailError != null,
            supportingText = emailError?.let {
                { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                // Clear error when user starts typing
                passwordError = null
            },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = passwordError != null,
            supportingText = passwordError?.let {
                { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        )

        // Forgot Password link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onNavigateToForgotPassword,
                modifier = Modifier.testTag("forgotPasswordButton")
            ) {
                Text(
                    text = "Forgot Password?",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                errorMessage = ""

                // Validate credentials before attempting login
                var isValid = true

                // Validate email
                if (!ValidationUtils.isValidEmail(email)) {
                    emailError = "Please enter a valid email address"
                    isValid = false
                }

                // Validate password
                if (!ValidationUtils.isValidPassword(password)) {
                    passwordError = "Password must be at least 6 characters"
                    isValid = false
                }

                if (!isValid) {
                    // Handle validation error
                    if (emailError == null && passwordError == null) {
                        errorMessage = "Invalid credentials"
                    }
                } else {
                    // Proceed with login if validation passes
                    authViewModel.login(email, password) { success, message ->
                        if (success) {
                            onLoginSuccess()
                        } else {
                            errorMessage = message ?: "Login failed"
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("loginButton"),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Login")
            }
        }

        TextButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Don't have an account? Register")
        }
    }
}
