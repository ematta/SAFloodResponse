package edu.utap.ui.screens

import android.util.Log
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.utap.auth.AuthState
import edu.utap.models.FirestoreUser
import edu.utap.ui.viewmodel.AuthViewModelInterface
import edu.utap.utils.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
        authState is AuthState.Idle.Authenticated &&
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

                // Add validation for empty password and email
                if (email.isEmpty()) {
                    emailError = "Email cannot be empty"
                }

                if (password.isEmpty()) {
                    passwordError = "Password cannot be empty"
                }

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
            enabled = authState !is AuthState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("loginButton")
        ) {
            if (authState is AuthState.Loading) {
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
                val isPasswordValid = ValidationUtils.isValidPassword("test123")
                // If all inputs are valid, attempt login
                if (isEmailValid && isPasswordValid) {
                    authViewModel.login("test@user.com", "test123") { success, message ->
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

//
// Preview for LoginScreen using a fake ViewModel and no-op lambdas
//

private class FakeAuthViewModel : AuthViewModelInterface {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState

    override fun register(
        email: String,
        password: String,
        name: String,
        role: String,
        function: (Boolean, String?) -> Unit
    ) {
        Log.d(
            "LoginScreenPreview",
            "Fake register called with email=$email, name=$name, role=$role"
        )
        function(false, "Preview mode: register not performed")
    }

    override fun login(email: String, password: String, function: (Boolean, String?) -> Unit) {
        Log.d("LoginScreenPreview", "Fake login called with email=$email, password=$password")
        function(false, "Preview mode: login not performed")
    }

    override fun logout(): AuthState.Idle.Unauthenticated {
        Log.d("LoginScreenPreview", "Fake logout called")
        return AuthState.Idle.Unauthenticated
    }

    override fun resetPassword(email: String, callback: (Boolean, String?) -> Unit) {
        Log.d("LoginScreenPreview", "Fake resetPassword called with email=$email")
        callback(false, "Preview mode: resetPassword not performed")
    }

    override fun getCurrentUser(): FirestoreUser? {
        Log.d("LoginScreenPreview", "Fake getCurrentUser called")
        return null
    }

    override fun restoreAuthState() {
        Log.d("LoginScreenPreview", "Fake restoreAuthState called")
    }

    override fun updateAuthState(sent: AuthState) {
        Log.d("LoginScreenPreview", "Fake updateAuthState called with $sent")
    }

    override fun isUserLoggedIn(): Boolean {
        return true
    }
}

@Preview(
    name = "LoginScreen Preview",
    showBackground = true,
    group = "Auth"
)
@Composable
fun LoginScreenPreview() {
    Log.d("LoginScreenPreview", "Preview composable invoked")
    LoginScreen(
        authViewModel = FakeAuthViewModel(),
        onNavigateToRegister = { Log.d("LoginScreenPreview", "Register clicked (preview)") },
        onNavigateToForgotPassword = {
            Log.d("LoginScreenPreview", "Forgot Password clicked (preview)")
        },
        onLoginSuccess = { Log.d("LoginScreenPreview", "Login success (preview)") }
    )
}
