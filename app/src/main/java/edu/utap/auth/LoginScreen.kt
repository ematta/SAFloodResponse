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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.auth.utils.ValidationUtils

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
    if (authState is AuthState.Authenticated) {
        onLoginSuccess()
        return
    }
    
    // Update error message when auth state contains an error
    if (authState is AuthState.Error) {
        errorMessage = (authState as AuthState.Error).message
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
                modifier = Modifier.padding(bottom = 16.dp)
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
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                errorMessage = ""
                
                // Validate credentials before attempting login
                val validationResult = ValidationUtils.validateLoginCredentials(email, password)
                if (!validationResult.first) {
                    // Handle validation error
                    val errorMsg = validationResult.second ?: "Invalid credentials"
                    
                    // Set specific field error based on validation result
                    when {
                        email.isBlank() || !ValidationUtils.validateEmail(email).first -> {
                            emailError = ValidationUtils.validateEmail(email).second
                        }
                        password.isBlank() || !ValidationUtils.validatePassword(password).first -> {
                            passwordError = ValidationUtils.validatePassword(password).second
                        }
                        else -> {
                            errorMessage = errorMsg
                        }
                    }
                } else {
                    // Proceed with login if validation passes
                    authViewModel.login(email, password)
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