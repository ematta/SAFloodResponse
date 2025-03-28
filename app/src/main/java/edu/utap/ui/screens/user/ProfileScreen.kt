package edu.utap.ui.screens.user

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import edu.utap.ui.components.*
import edu.utap.ui.viewmodel.UserViewModel
import edu.utap.user.UserProfile
import edu.utap.user.UserProfileState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(userViewModel: UserViewModel = viewModel(), onNavigateBack: () -> Unit = {}) {
    val profileState by userViewModel.profileState.collectAsStateWithLifecycle()
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            userViewModel.getUserProfile(uid)
        }
    }

    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var photoUrl by rememberSaveable { mutableStateOf("") }

    var isEditing by rememberSaveable { mutableStateOf(false) }

    // Update local state when profile data changes
    LaunchedEffect(profileState) {
        if (profileState is edu.utap.user.UserProfileState.Success) {
            val profile = (profileState as edu.utap.user.UserProfileState.Success).profile
            displayName = profile.displayName ?: ""
            email = profile.email ?: ""
            phoneNumber = profile.phoneNumber ?: ""
            address = profile.address ?: ""
            photoUrl = profile.photoUrl ?: ""
        }
    }

    // Setup image picker
    val context = LocalContext.current
    val photoFile = remember { File(context.cacheDir, "profile_photo.jpg") }
    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "edu.utap.flood.fileprovider",
            photoFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                userViewModel.uploadProfileImage(
                    uploadContext = context, imageUri = photoUri, uid = currentUser!!.uid)
            }
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { it ->
                userViewModel.uploadProfileImage(
                    uploadContext = context, imageUri = it, uid = currentUser!!.uid)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // Back icon
                    }
                },
                actions = {
                    // Edit button
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            imageVector = Icons.TwoTone.Edit,
                            contentDescription = if (isEditing) "Save" else "Edit"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile loading state
            when (profileState) {
                is UserProfileState.Loading -> {
                    CircularProgressIndicator()
                }
                is UserProfileState.Error -> {
                    val error = (profileState as UserProfileState.Error).toString()
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is UserProfileState.Success -> {
                    // Profile photo
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        // Profile image
                        if (photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineLarge
                                )
                            }
                        }

                        // Camera button (only visible in edit mode)
                        if (isEditing) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(
                                    onClick = { cameraLauncher.launch(photoUri) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.TwoTone.PlayArrow, "Take photo")
                                }
                                
                                IconButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.TwoTone.PlayArrow, "Choose from gallery")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Profile information fields
                    ProfileField(
                        label = "Name",
                        value = displayName,
                        onValueChange = { displayName = it },
                        isEditing = isEditing
                    )
                    
                    ProfileField(
                        label = "Email",
                        value = email,
                        onValueChange = { email = it },
                        isEditing = false, // Email should not be editable
                        keyboardType = KeyboardType.Email
                    )
                    
                    ProfileField(
                        label = "Phone",
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        isEditing = isEditing,
                        keyboardType = KeyboardType.Phone
                    )
                    
                    ProfileField(
                        label = "Address",
                        value = address,
                        onValueChange = { address = it },
                        isEditing = isEditing
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Save button (only visible in edit mode)
                    if (isEditing) {
                        Button(
                            onClick = {
                                val profile = UserProfile(
                                    displayName = displayName,
                                    phoneNumber = phoneNumber,
                                    address = address
                                )
                                userViewModel.updateUserProfile(profile)
                                isEditing = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Changes")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign out button
                    OutlinedButton(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out")
                    }
                }

                UserProfileState.Idle.Initial -> TODO()
                is UserProfileState.Idle.Loaded -> TODO()
                is UserProfileState.Idle.Updated -> TODO()
            }
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditing: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = value.ifEmpty { "Not set" },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Divider()
        }
    }
}

@Composable
private fun Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content
    )
}
