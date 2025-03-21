package edu.utap.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.auth.utils.ValidationUtils

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModelInterface = viewModel<AuthViewModel>(),
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
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
    if (authState is AuthState.Authenticated) {
        onRegisterSuccess()
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
            text = "Create an Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Display error message if any
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { 
                name = it 
                nameError = null
            },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = nameError != null,
            supportingText = nameError?.let { 
                { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it 
                emailError = null
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            isError = emailError != null,
            supportingText = emailError?.let { 
                { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it 
                passwordError = null
                // Clear confirm password error if it was a mismatch
                if (confirmPassword.isNotEmpty() && confirmPassword == password) {
                    confirmPasswordError = null
                }
            },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = passwordError != null,
            supportingText = passwordError?.let { 
                { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Confirm password field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { 
                confirmPassword = it 
                confirmPasswordError = null
            },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = confirmPasswordError != null,
            supportingText = confirmPasswordError?.let { 
                { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Register button
        Button(
            onClick = {
                errorMessage = ""
                nameError = null
                emailError = null
                passwordError = null
                confirmPasswordError = null
                
                // Validate input using ValidationUtils
                var isValid = true
                
                // Validate name
                if (name.isBlank()) {
                    nameError = "Please enter your name"
                    isValid = false
                }
                
                // Validate email
                val emailValidation = ValidationUtils.validateEmail(email)
                if (!emailValidation.first) {
                    emailError = emailValidation.second
                    isValid = false
                }
                
                // Validate password
                val passwordValidation = ValidationUtils.validatePassword(password)
                if (!passwordValidation.first) {
                    passwordError = passwordValidation.second
                    isValid = false
                }
                
                // Validate password confirmation
                if (password != confirmPassword) {
                    confirmPasswordError = "Passwords do not match"
                    isValid = false
                }
                
                // If all validations pass, proceed with registration
                if (isValid) {
                    authViewModel.register(email, password, name)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            }
            Text("Register")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Login navigation link
        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Log in")
        }
    }
}