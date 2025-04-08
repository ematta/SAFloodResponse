package edu.utap.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
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
import edu.utap.auth.model.AuthViewModel
import edu.utap.auth.model.AuthViewModelInterface
import edu.utap.utils.RoleUtils
import edu.utap.utils.ValidationUtils

/**
 * Composable screen for user self-registration with email, password, and name.
 *
 * This screen provides input fields for user details, performs validation,
 * displays error messages, and triggers registration via the provided ViewModel.
 * It also provides navigation back to the login screen.
 *
 * @param authViewModel The authentication ViewModel interface used to perform registration.
 * @param onNavigateToLogin Callback invoked to navigate back to the login screen.
 * @param onRegisterSuccess Callback invoked when registration is successful.
 */
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
    if (authState is AuthState.Idle.Authenticated) {
        onRegisterSuccess()
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
                if (!ValidationUtils.isValidEmail(email)) {
                    emailError = "Please enter a valid email address"
                    isValid = false
                }

                // Validate password
                if (!ValidationUtils.isValidPassword(password)) {
                    passwordError = "Password must be at least 6 characters"
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

/**
 * Special registration screen with role selection for admin use.
 *
 * This version of the registration screen includes a role selector
 * and should only be accessible to admin users for creating accounts
 * with specific roles.
 */
@Composable
fun AdminRegisterScreen(
    authViewModel: AuthViewModelInterface = viewModel<AuthViewModel>(),
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(RoleUtils.ROLE_REGULAR) }
    var errorMessage by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val roles = RoleUtils.getAllRoles()
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Check if admin is creating an account for someone else
    if (authState is AuthState.Idle.Authenticated) {
        val viewModel = authViewModel as AuthViewModel
        if (!viewModel.hasPermission(RoleUtils.ROLE_ADMIN)) {
            // Non-admin user shouldn't be here, redirect
            onNavigateBack()
            return
        }
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
            text = "Create a New User Account",
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
            isError = confirmPasswordError != null,
            supportingText = confirmPasswordError?.let {
                { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Role selection
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedRole,
                onValueChange = { },
                label = { Text("User Role") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { dropdownExpanded = true }) {
                        // Icon for dropdown
                    }
                },
                supportingText = {
                    Text(text = RoleUtils.roleDescriptions[selectedRole] ?: "")
                }
            )

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                roles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role) },
                        onClick = {
                            selectedRole = role
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

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
                    nameError = "Please enter a name"
                    isValid = false
                }

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

                // Validate password confirmation
                if (password != confirmPassword) {
                    confirmPasswordError = "Passwords do not match"
                    isValid = false
                }

                // If all validations pass, proceed with registration
                if (isValid) {
                    authViewModel.register(email, password, name, selectedRole)
                    onRegisterSuccess()
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
            Text("Create User Account")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cancel button
        TextButton(onClick = onNavigateBack) {
            Text("Cancel")
        }
    }
}
