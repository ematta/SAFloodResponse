package edu.utap.ui.screens.flood

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import edu.utap.R
import edu.utap.ui.viewmodel.FloodReportViewModel
import edu.utap.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Composable function that displays the flood report form screen.
 *
 * This screen allows users to:
 * - Enter contact information (email and phone)
 * - Upload photos
 * - Answer questions about flood severity and office access
 * - Submit the report
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION])
fun FloodReportFormScreen(
    viewModel: FloodReportViewModel,
    onNavigateBack: () -> Unit
) {
    // Collect state from ViewModel
    val email by viewModel.email.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val selectedPhotos by viewModel.selectedPhotos.collectAsState()
    val severity by viewModel.severity.collectAsState()
    val waterDepth by viewModel.waterDepth.collectAsState()
    val isRoadClosed by viewModel.isRoadClosed.collectAsState()
    val canAccessOffice by viewModel.canAccessOffice.collectAsState()
    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()
    val description by viewModel.description.collectAsState()

    // Local UI state
    var uploadingPhoto by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isGettingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Function to request current location and update fields
    val getCurrentLocation: () -> Unit = {
        isGettingLocation = true
        locationError = null
        viewModel.viewModelScope.launch {
            try {
                val location = viewModel.locationUtils.getCurrentLocation()
                location?.let {
                    viewModel.updateManualLocation(it.latitude, it.longitude)
                } ?: run {
                    locationError = "Couldn't get location. Please try again."
                }
            } catch (e: Exception) {
                locationError = e.message ?: "Error getting location"
            } finally {
                isGettingLocation = false
            }
        }
    }

    // Create camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && cameraPhotoUri != null) {
                uploadingPhoto = true
                uploadError = null
                viewModel.uploadFloodReportPhoto(
                    context = context,
                    imageUri = cameraPhotoUri!!,
                    reportId = UUID.randomUUID().toString()
                ) { result ->
                    uploadingPhoto = false
                    result.onFailure { error ->
                        uploadError = error.message
                    }
                }
            }
        }
    )

    // Create gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                uploadingPhoto = true
                uploadError = null
                viewModel.uploadFloodReportPhoto(
                    context = context,
                    imageUri = uri,
                    reportId = UUID.randomUUID().toString()
                ) { result ->
                    uploadingPhoto = false
                    result.onFailure { error ->
                        uploadError = error.message
                    }
                }
            }
        }
    )

    // Create a temp file for camera photos
    fun createImageFile(): Uri? {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            
            // Make sure the directory exists
            if (storageDir == null) {
                uploadError = "Could not access storage directory"
                return null
            }
            
            if (!storageDir.exists()) {
                val created = storageDir.mkdirs()
                if (!created) {
                    uploadError = "Could not create storage directory"
                    return null
                }
            }
            
            val imageFile = File.createTempFile(
                "FLOOD_${timestamp}_",
                ".jpg",
                storageDir
            )
            
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: Exception) {
            uploadError = "Could not create image file: ${e.message}"
            return null
        }
    }

    FloodReportFormScreenContent(
        email = email,
        phoneNumber = phoneNumber,
        severity = severity,
        waterDepth = waterDepth,
        isRoadClosed = isRoadClosed,
        canAccessOffice = canAccessOffice,
        latitude = latitude,
        longitude = longitude,
        description = description,
        photos = selectedPhotos,
        isUploadingPhoto = uploadingPhoto,
        photoUploadError = uploadError,
        isGettingLocation = isGettingLocation,
        locationError = locationError,
        onLatitudeChange = viewModel::updateLatitude,
        onLongitudeChange = viewModel::updateLongitude,
        onGetCurrentLocation = getCurrentLocation,
        onEmailChange = { viewModel.updateEmail(it) },
        onPhoneNumberChange = { viewModel.updatePhoneNumber(it) },
        onSeverityChange = { viewModel.updateSeverity(it) },
        onWaterDepthChange = { viewModel.updateWaterDepth(it) },
        onRoadClosedChange = { viewModel.updateRoadClosed(it) },
        onOfficeAccessChange = { viewModel.updateOfficeAccess(it) },
        onDescriptionChange = { viewModel.updateDescription(it) },
        onRemovePhoto = { viewModel.removePhoto(it) },
        onCameraClick = {
            // Attempt to create image file and launch camera
            val uri = createImageFile()
            if (uri != null) {
                cameraPhotoUri = uri
                cameraLauncher.launch(uri)
            }
        },
        onGalleryClick = {
            galleryLauncher.launch("image/*")
        },
        onSubmit = {
            viewModel.submitReport()
            onNavigateBack()
        },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloodReportFormScreenContent(
    email: String,
    phoneNumber: String,
    severity: String,
    waterDepth: Double,
    isRoadClosed: Boolean?,
    canAccessOffice: Boolean?,
    latitude: Double,
    longitude: Double,
    description: String,
    photos: List<String>,
    isUploadingPhoto: Boolean,
    photoUploadError: String?,
    isGettingLocation: Boolean = false,
    locationError: String? = null,
    onLatitudeChange: (Double) -> Unit,
    onLongitudeChange: (Double) -> Unit,
    onGetCurrentLocation: () -> Unit = {},
    onEmailChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onSeverityChange: (String) -> Unit,
    onWaterDepthChange: (Double) -> Unit,
    onRoadClosedChange: (Boolean) -> Unit,
    onOfficeAccessChange: (Boolean) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onSubmit: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF8C9EFF),
                shadowElevation = 4.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Report",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Emergency and Flood Report Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Smiley Face Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE6E9FF))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_flood_report),
                        contentDescription = "Smiley Face",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Emergency",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Flood report",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Enter your email*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = Color(0xFF8C9EFF)
                )
            )

            // Phone Number Input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                label = { Text("Enter your phone number*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = Color(0xFF8C9EFF)
                )
            )

            // Latitude Input with Location Button
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = latitude.toString(),
                    onValueChange = { onLatitudeChange(it.toDoubleOrNull() ?: 0.0) },
                    label = { Text("Latitude*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.LightGray,
                        focusedBorderColor = Color(0xFF8C9EFF)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = onGetCurrentLocation) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_add_location_24),
                                contentDescription = "Get current location",
                                tint = Color(0xFF8C9EFF)
                            )
                        }
                    }
                )
                
                if (isGettingLocation) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        color = Color(0xFF8C9EFF)
                    )
                }
                
                if (locationError != null) {
                    Text(
                        text = locationError,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }

            // Longitude Input with Location Button
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = longitude.toString(),
                    onValueChange = { onLongitudeChange(it.toDoubleOrNull() ?: 0.0) },
                    label = { Text("Longitude*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.LightGray,
                        focusedBorderColor = Color(0xFF8C9EFF)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = onGetCurrentLocation) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_add_location_24),
                                contentDescription = "Get current location",
                                tint = Color(0xFF8C9EFF)
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Your Recent Photos Section
            Text(
                text = "Your recent",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(photos) { photo ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                            .clickable { onRemovePhoto(photo) }
                    ) {
                        AsyncImage(
                            model = photo,
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE6E9FF))
                            .clickable { onCameraClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add photo",
                            tint = Color(0xFF8C9EFF)
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE6E9FF))
                            .clickable { onGalleryClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Select from gallery",
                            tint = Color(0xFF8C9EFF)
                        )
                    }
                }
            }

            if (isUploadingPhoto) {
                Text(
                    text = "Uploading photo...",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            if (photoUploadError != null) {
                Text(
                    text = "Error: $photoUploadError",
                    fontSize = 14.sp,
                    color = Color.Red
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Flood Severity Question
            Text(
                text = "Flood Severity Level*",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Low option
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = severity == "low",
                        onClick = { onSeverityChange("low") },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF8C9EFF)
                        )
                    )
                    Text(text = "Low")
                }

                // Medium option
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = severity == "medium",
                        onClick = { onSeverityChange("medium") },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF8C9EFF)
                        )
                    )
                    Text(text = "Medium")
                }

                // High option
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = severity == "high",
                        onClick = { onSeverityChange("high") },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF8C9EFF)
                        )
                    )
                    Text(text = "High")
                }
            }

            // Water Depth Input
            OutlinedTextField(
                value = waterDepth.toString(),
                onValueChange = { text -> onWaterDepthChange(text.toDoubleOrNull() ?: 0.0) },
                label = { Text("Water Depth (inches)*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = Color(0xFF8C9EFF)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Road Closure Question
            Text(
                text = "Is the road closed due to flooding?*",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Yes option
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isRoadClosed == true,
                        onClick = { onRoadClosedChange(true) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF8C9EFF)
                        )
                    )
                    Text(text = "Yes")
                }

                // No option
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isRoadClosed == false,
                        onClick = { onRoadClosedChange(false) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF8C9EFF)
                        )
                    )
                    Text(text = "No")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Office Access Question
            Text(
                text = "Can you access the office today?*",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Yes option
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = canAccessOffice == true,
                        onClick = { onOfficeAccessChange(true) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF8C9EFF)
                        )
                    )
                    Text(text = "Yes")
                }

                // No option
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = canAccessOffice == false,
                        onClick = { onOfficeAccessChange(false) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF8C9EFF)
                        )
                    )
                    Text(text = "No")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8C9EFF)
                ),
                // Disable button if required fields are not filled
                enabled = email.isNotEmpty() &&
                    phoneNumber.isNotEmpty() &&
                    severity.isNotEmpty() &&
                    waterDepth > 0 &&
                    canAccessOffice != null &&
                    isRoadClosed != null
            ) {
                Text(
                    text = "Submit now",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Preview of FloodReportFormScreen for Compose tooling.
 * Uses sample data and no-op callbacks to comply with @Preview requirements.
 */
@Preview(
    name = "Flood Report Form",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun PreviewFloodReportFormScreen() {
    FloodReportFormScreenContent(
        email = "test@example.com",
        phoneNumber = "123-456-7890",
        severity = "medium",
        waterDepth = 12.0,
        isRoadClosed = false,
        canAccessOffice = true,
        latitude = 37.7749,
        longitude = -122.4194,
        description = "",
        photos = emptyList(),
        isUploadingPhoto = false,
        photoUploadError = null,
        onLatitudeChange = {},
        onLongitudeChange = {},
        onEmailChange = {},
        onPhoneNumberChange = {},
        onSeverityChange = {},
        onWaterDepthChange = {},
        onRoadClosedChange = {},
        onOfficeAccessChange = {},
        onDescriptionChange = {},
        onRemovePhoto = {},
        onCameraClick = {},
        onGalleryClick = {},
        onSubmit = {},
        onNavigateBack = {}
    )
}
