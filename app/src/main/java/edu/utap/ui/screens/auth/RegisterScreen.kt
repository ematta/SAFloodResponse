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
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.auth.AuthState
import edu.utap.auth.model.AuthViewModel
import edu.utap.auth.model.AuthViewModelInterface
import edu.utap.utils.RoleUtils
import edu.utap.utils.ValidationUtils
import android.util.Log

// Test tags for UI testing
const val REGISTER_NAME_FIELD_TAG = "edu.utap.auth.nameField"
const val REGISTER_EMAIL_FIELD_TAG = "edu.utap.auth.emailField"
const val REGISTER_PASSWORD_FIELD_TAG = "edu.utap.auth.passwordField"
const val REGISTER_CONFIRM_PASSWORD_FIELD_TAG = "edu.utap.auth.confirmPasswordField"
const val REGISTER_BUTTON_TAG = "edu.utap.auth.registerButton"

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModelInterface = viewModel<AuthViewModel>(),
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    Log.d("RegisterScreen: Composable called with viewModel: $authViewModel")
    val authState by authViewModel.authState.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Track validation errors for each field
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // Check if user is already authenticated
    if (authState is AuthState.Idle.Authenticated) {
        onRegisterSuccess()
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
            text = "Register",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = null // Clear error when user types
            },
            label = { Text("Full Name") },
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag(REGISTER_NAME_FIELD_TAG)
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
                .testTag(REGISTER_EMAIL_FIELD_TAG),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null // Clear error when user types
                // Also clear confirm password error as it depends on password
                if (confirmPassword.isNotEmpty() && it == confirmPassword) {
                    confirmPasswordError = null
                }
            },
            label = { Text("Password") },
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it) } },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag(REGISTER_PASSWORD_FIELD_TAG),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        // Confirm Password field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                confirmPasswordError = null // Clear error when user types
            },
            label = { Text("Confirm Password") },
            isError = confirmPasswordError != null,
            supportingText = confirmPasswordError?.let { { Text(it) } },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag(REGISTER_CONFIRM_PASSWORD_FIELD_TAG),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        // Register button
        Button(
            onClick = {
                // Validate inputs
                val isNameValid = name.isNotEmpty()
                val isEmailValid = ValidationUtils.isValidEmail(email)
                val isPasswordValid = ValidationUtils.isValidPassword(password)
                val doPasswordsMatch = password == confirmPassword

                if (!isNameValid) {
                    nameError = "Please enter your name"
                }

                if (!isEmailValid) {
                    emailError = "Please enter a valid email address"
                }

                if (!isPasswordValid) {
                    passwordError = "Password must be at least 6 characters"
                }

                if (!doPasswordsMatch) {
                    confirmPasswordError = "Passwords do not match"
                }

                // If all inputs are valid, attempt registration
                if (isNameValid && isEmailValid && isPasswordValid && doPasswordsMatch) {
                    authViewModel.register(name, email, password, RoleUtils.ROLE_REGULAR) {
                            success,
                            message
                        ->
                        if (success) {
                            onRegisterSuccess()
                        } else {
                            errorMessage = message ?: "Registration failed"
                        }
                    }
                }
            },
            enabled = authState !is AuthState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(REGISTER_BUTTON_TAG)
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Register")
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

        // Login link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Already have an account?")
            TextButton(onClick = onNavigateToLogin) {
                Text("Login")
            }
        }
    }
}
