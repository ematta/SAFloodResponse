package edu.utap.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import edu.utap.auth.model.AuthViewModel
import edu.utap.auth.model.AuthViewModelInterface
import edu.utap.utils.ValidationUtils

/**
 * LoginScreen is a composable function that displays the login screen for the application.
 * It allows users to enter their email and password to log in.
 *
 * @param authViewModel The ViewModel responsible for authentication logic.
 * @param onNavigateToRegister Callback to navigate to the registration screen.
 * @param onNavigateToForgotPassword Callback to navigate to the forgot password screen.
 * @param onLoginSuccess Callback to be invoked when login is successful.
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModelInterface,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    requireNotNull(onNavigateToRegister) { "onNavigateToRegister cannot be null" }
    requireNotNull(onNavigateToForgotPassword) { "onNavigateToForgotPassword cannot be null" }
    requireNotNull(onLoginSuccess) { "onLoginSuccess cannot be null" }

    val authState by authViewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Track validation errors for each field
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Check if user is already authenticated
    if (
        authState is edu.utap.auth.AuthState.Idle.Authenticated &&
        authViewModel.getCurrentUser() != null
    ) {
        onLoginSuccess()
        return
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
                .padding(bottom = 8.dp)
                .testTag("emailField"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null // Clear error when user types
            },
            label = { Text("Password") },
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it) } },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("passwordField"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        // Login button
        Button(
            onClick = {
                // Validate inputs
                val isEmailValid = ValidationUtils.isValidEmail(email)
                val isPasswordValid = ValidationUtils.isValidPassword(password)

                if (!isEmailValid) {
                    emailError = "Please enter a valid email address"
                }

                if (!isPasswordValid) {
                    passwordError = "Password must be at least 6 characters"
                }

                // If all inputs are valid, attempt login
                if (isEmailValid && isPasswordValid) {
                    authViewModel.login(email, password) { success, message ->
                        if (success) {
                            onLoginSuccess()
                        } else {
                            errorMessage = message ?: "Login failed"
                        }
                    }
                }
            },
            enabled = authState !is edu.utap.auth.AuthState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("loginButton")
        ) {
            if (authState is edu.utap.auth.AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login")
            }
        }

        // Bypass button
        Button(
            onClick = {
                // Validate inputs
                val isEmailValid = ValidationUtils.isValidEmail("test@user.com")
                val isPasswordValid = ValidationUtils.isValidPassword("password123")
                // If all inputs are valid, attempt login
                if (isEmailValid && isPasswordValid) {
                    authViewModel.login("test@user.com", "password123") { success, message ->
                        if (success) {
                            onLoginSuccess()
                        } else {
                            errorMessage = message ?: "Login failed"
                        }
                    }
                }
            },
            enabled = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp) // Add padding to separate it from other elements
                .testTag("bypassButton")
        ) {
            Text("Bypass")
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

        // Register link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Don't have an account?")
            TextButton(onClick = onNavigateToRegister) {
                Text("Register")
            }
        }

        // Forgot password link
        TextButton(
            onClick = onNavigateToForgotPassword,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Forgot Password?")
        }
    }
}
