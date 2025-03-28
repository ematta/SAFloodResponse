package edu.utap.ui.screens

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

    LaunchedEffect(profileState) {
        if (profileState is UserProfileState.Success) {
            val profile = (profileState as UserProfileState.Success).profile
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
                    LoadingIndicator()
                }
                is UserProfileState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ErrorMessage(message = "Holder")

                        SpacerHeight(16)

                        AppButton(
                            text = "Create Profile",
                            onClick = {
                                currentUser?.uid?.let { uid ->
                                    // Create a default profile if none exists
                                    val newProfile = UserProfile(
                                        uid = uid,
                                        displayName = currentUser.displayName ?: "",
                                        email = currentUser.email ?: ""
                                    )
                                    userViewModel.createUserProfile(newProfile)
                                }
                            }
                        )
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
                                            userViewModel.uploadProfileImage(
                                                uploadContext = context,
                                                imageUri = uri,
                                                uid = uid)
                                        }
                                    }
                                )
                            }

                            AppTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = "Display Name"
                            )

                            AppTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = "Email",
                                enabled = false // Email changes require re-authentication
                            )

                            AppTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = "Phone Number",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )

                            AppTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = "Address"
                            )

                            AppButton(
                                text = "Save",
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
                                        userViewModel.updateUserProfile(updatedProfile)
                                        isEditing = false
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            )
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
fun ProfileRow(label: String, value: String) {
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

@Composable
fun ProfileImagePicker(photoUrl: String, onImageSelected: (Uri) -> Unit) {
    val context = LocalContext.current
    val file = remember { File.createTempFile("profile_", ".jpg", context.cacheDir) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) onImageSelected(uri)
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let(onImageSelected)
        }
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (photoUrl.isNotEmpty()) {
            // Existing image loading logic
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = { cameraLauncher.launch(uri) }) {
                    Icon(Icons.TwoTone.PlayArrow, "Take Photo")
                }
                Text("OR", style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Icon(Icons.TwoTone.Edit, "Choose from Gallery")
                }
            }
        }
    }
}
