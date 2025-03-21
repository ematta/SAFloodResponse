package edu.utap.ui.screens.profile

import android.Manifest
import android.net.Uri
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import edu.utap.auth.UserProfileState
import edu.utap.di.ViewModelFactory
import edu.utap.models.UserProfile
import edu.utap.ui.components.*
import edu.utap.ui.viewmodel.UserViewModel
import edu.utap.utils.DefaultImageCacheProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
/**
 * User profile screen composable.
 *
 * Displays and allows editing of the user's profile information,
 * including display name, email, phone number, address, and profile picture.
 *
 * @param userViewModel The [UserViewModel] managing profile data.
 * @param onNavigateBack Callback invoked when navigating back.
 */
@Composable
fun ProfileScreen(
    userViewModel: UserViewModel = ViewModelFactory(context = LocalContext.current).create(UserViewModel::class.java),
    onNavigateBack: () -> Unit = {},
    onNavigateAdmin: () -> Unit = {}
) {
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
    var role by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(profileState) {
        if (profileState is UserProfileState.Success) {
            val profile = (profileState as UserProfileState.Success).profile
            displayName = profile.displayName
            email = profile.email
            phoneNumber = profile.phoneNumber
            address = profile.address
            photoUrl = profile.photoUrl
            role = profile.role
        }
    }

    var isEditing by rememberSaveable { mutableStateOf(false) }

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
                        TextButton(onClick = { isEditing = false }) { Text("Cancel") }
                    } else {
                        TextButton(onClick = { isEditing = true }) { Text("Edit") }
                        TextButton(onClick = onNavigateAdmin) { Text("Admin") }
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
                                                uid = uid
                                            )
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

                            AppTextField(
                                value = role,
                                onValueChange = {},
                                label = "Role",
                                enabled = false
                            )

                            AppButton(
                                text = "Save",
                                onClick = {
                                    currentUser?.uid?.let { uid ->
                                        // Always include all fields, even if blank
                                        val updatedProfile = UserProfile(
                                            uid = uid,
                                            displayName = displayName,
                                            email = email,
                                            photoUrl = photoUrl,
                                            phoneNumber = phoneNumber,
                                            address = address,
                                            role = role // preserve existing role
                                        )
                                        userViewModel.updateUserProfile(updatedProfile)
                                        isEditing = false
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            )
                        } else {
                            // View mode
                            // Display profile image at the top in view mode
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (photoUrl.isNotEmpty()) {
                                    CacheAwareAsyncImage(
                                        photoUrl = photoUrl,
                                        contentDescription = "Profile Image",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No Image", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

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

                            ProfileRow(
                                label = "Role",
                                value = role
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays a labeled row of profile information with a divider.
 *
 * @param label The label for the information.
 * @param value The value to display.
 */
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

/**
 * Profile image picker composable with camera and gallery options.
 *
 * @param photoUrl URL of the current profile image.
 * @param onImageSelected Callback invoked with the selected image URI.
 */
@Composable
fun ProfileImagePicker(photoUrl: String, onImageSelected: (Uri) -> Unit) {
    val context = LocalContext.current
    val file = remember { File.createTempFile("profile_", ".jpg", context.cacheDir) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    // ActivityResult launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> if (success) onImageSelected(uri) }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { selectedUri -> selectedUri?.let(onImageSelected) }
    )
    
    // Permission request launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) cameraLauncher.launch(uri)
            else Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    )
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) galleryLauncher.launch("image/*")
            else Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (photoUrl.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CacheAwareAsyncImage(
                    photoUrl = photoUrl,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable { 
                            // Allow choice to replace the image through a context menu
                            val options = arrayOf("Take New Photo", "Choose from Gallery")
                            androidx.appcompat.app.AlertDialog.Builder(context)
                                .setTitle("Update Profile Photo")
                                .setItems(options) { _, which ->
                                    when (which) {
                                        0 -> {
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                cameraLauncher.launch(uri)
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }
                                        1 -> {
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                                galleryLauncher.launch("image/*")
                                            } else {
                                                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                            }
                                        }
                                    }
                                }
                                .show()
                        },
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = {
                    // Camera intent with permission check
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Icon(Icons.TwoTone.PlayArrow, "Take Photo")
                }
                Text("OR", style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = {
                    // Gallery intent with permission check
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        galleryLauncher.launch("image/*")
                    } else {
                        storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }) {
                    Icon(Icons.TwoTone.Edit, "Choose from Gallery")
                }
            }
        }
    }
}

/**
 * Previews for Jetpack Compose tooling.
 * These previews use sample/mock data and avoid real ViewModel or navigation dependencies,
 * in accordance with official Compose @Preview best practices.
 */

@Preview(name = "Profile Row", showBackground = true)
@Composable
fun ProfileRowPreview() {
    ProfileRow(label = "Email", value = "user@example.com")
}

@Preview(name = "Profile Image Picker", showBackground = true)
@Composable
fun ProfileImagePickerPreview() {
    // Use empty photoUrl and a no-op callback for preview
    ProfileImagePicker(photoUrl = "", onImageSelected = {})
}

@Preview(name = "Profile Screen (View Mode)", showBackground = true)
@Composable
fun ProfileScreenPreview() {
    // Simulate the view mode of ProfileScreen with sample data
    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileRow(label = "Display Name", value = "Jane Doe")
                ProfileRow(label = "Email", value = "jane.doe@example.com")
                ProfileRow(label = "Phone Number", value = "+1 555-123-4567")
                ProfileRow(label = "Address", value = "123 Main St, Springfield")
            }
        }
    }
}
