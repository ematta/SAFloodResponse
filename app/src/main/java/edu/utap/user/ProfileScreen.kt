package edu.utap.user

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userViewModel: UserViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModelWithContext = remember { userViewModel.apply { this.context = context } }
    val profileState by viewModelWithContext.profileState.collectAsStateWithLifecycle()
    val currentUser = FirebaseAuth.getInstance().currentUser
    
    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            viewModelWithContext.getUserProfile(uid)
        }
    }
    
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    
    var isEditing by remember { mutableStateOf(false) }
    
    LaunchedEffect(profileState) {
        if (profileState is UserProfileState.Success) {
            val profile = (profileState as UserProfileState.Success).userProfile
            displayName = profile.displayName
            email = profile.email
            phoneNumber = profile.phoneNumber
            address = profile.address
            photoUrl = profile.photoUrl
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // Use your back icon here
                    }
                },
                actions = {
                    if (isEditing) {
                        TextButton(onClick = { isEditing = false }) {
                            Text("Cancel")
                        }
                    } else {
                        TextButton(onClick = { isEditing = true }) {
                            Text("Edit")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (profileState) {
                is UserProfileState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UserProfileState.Error -> {
                    val errorMessage = (profileState as UserProfileState.Error).message
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(onClick = {
                            currentUser?.uid?.let { uid ->
                                // Create a default profile if none exists
                                val newProfile = UserProfile(
                                    uid = uid,
                                    displayName = currentUser.displayName ?: "",
                                    email = currentUser.email ?: ""
                                )
                                viewModelWithContext.createUserProfile(newProfile)
                            }
                        }) {
                            Text("Create Profile")
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isEditing) {
                            // Edit mode
                            val context = LocalContext.current
                            
                            // Profile image picker
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ProfileImagePicker(
                                    photoUrl = photoUrl,
                                    onImageSelected = { uri ->
                                        currentUser?.uid?.let { uid ->
                                            viewModelWithContext.uploadProfileImage(context, uri, uid)
                                        }
                                    }
                                )
                            }
                            
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Display Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false // Email changes require re-authentication
                            )
                            
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Address") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Button(
                                onClick = {
                                    currentUser?.uid?.let { uid ->
                                        val updatedProfile = UserProfile(
                                            uid = uid,
                                            displayName = displayName,
                                            email = email,
                                            photoUrl = photoUrl,
                                            phoneNumber = phoneNumber,
                                            address = address
                                        )
                                        viewModelWithContext.updateUserProfile(updatedProfile)
                                        isEditing = false
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Save")
                            }
                        } else {
                            // View mode
                            ProfileRow(
                                label = "Display Name",
                                value = displayName
                            )
                            
                            ProfileRow(
                                label = "Email",
                                value = email
                            )
                            
                            ProfileRow(
                                label = "Phone Number",
                                value = phoneNumber
                            )
                            
                            ProfileRow(
                                label = "Address",
                                value = address
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        
        Text(
            text = value.ifEmpty { "Not provided" },
            style = MaterialTheme.typography.bodyLarge
        )
        
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}