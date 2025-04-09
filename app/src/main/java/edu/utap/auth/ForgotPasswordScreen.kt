package edu.utap.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.utap.models.AuthViewModelInterface

/**
 * Composable screen for initiating a password reset via email.
 *
 * This screen allows users to enter their email address and request a password reset link.
 * It performs email validation, displays success or error messages, and navigates back
 * to the login screen upon successful request.
 *
 * @param viewModel The authentication ViewModel interface used to perform password reset.
 * @param onNavigateToLogin Callback invoked to navigate back to the login screen.
 */
@Composable
fun ForgotPasswordScreen(viewModel: AuthViewModelInterface, onNavigateToLogin: () -> Unit) {
    val authState by viewModel.authState.collectAsState()

    val emailMessage = when (authState) {
        is AuthState.Idle.PasswordResetSent -> "Password reset email sent. Check your inbox."
        else -> ""
    }

    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }

    val isEmailValid = remember(email) {
        email = email.trim()
        email.isNotEmpty() &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    // Function to validate form and submit
    val validateAndSendResetEmail = {
        var isValid = true

        // Validate email
        if (!isEmailValid) {
            emailError = "Please enter a valid email address"
            isValid = false
        }

        if (isValid) {
            viewModel.resetPassword(email, { success, message ->
                if (success) {
                    viewModel.updateAuthState(AuthState.Idle.PasswordResetSent)
                } else {
                    viewModel.updateAuthState(AuthState.Error.Generic(message ?: "Unknown error"))
                }
            })
        }

        isValid
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Idle.PasswordResetSent -> {
                // Navigate back to login screen when password reset email is sent
                onNavigateToLogin()
            }
            else -> { /* Do nothing */ }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            IconButton(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Login"
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Forgot Password",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = emailMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                    },
                    label = { Text("Email") },
                    singleLine = true,
                    isError = emailError != null,
                    supportingText = {
                        emailError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Icon"
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            validateAndSendResetEmail()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Reset Password Button
                Button(
                    onClick = { validateAndSendResetEmail() },
                    enabled = authState !is AuthState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(top = 8.dp)
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Send Reset Link")
                    }
                }

                // Error Message
                if (authState is AuthState.Error) {
                    val errorMessage = when (authState) {
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
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
