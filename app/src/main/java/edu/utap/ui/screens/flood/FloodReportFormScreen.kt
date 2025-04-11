package edu.utap.ui.screens.flood

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import edu.utap.R
import edu.utap.ui.viewmodel.FloodReportViewModel

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
fun FloodReportFormScreen(viewModel: FloodReportViewModel, onNavigateBack: () -> Unit) {
    // Collect state from ViewModel
    val email by viewModel.email.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val selectedPhotos by viewModel.selectedPhotos.collectAsState()
    val severity by viewModel.severity.collectAsState()
    val waterDepth by viewModel.waterDepth.collectAsState()
    val isRoadClosed by viewModel.isRoadClosed.collectAsState()
    val canAccessOffice by viewModel.canAccessOffice.collectAsState()

    // Local UI state
    var selectedImage by remember { mutableStateOf<String?>(null) }

    // Set selected image from photos if available
    LaunchedEffect(selectedPhotos) {
        if (selectedPhotos.isNotEmpty() && selectedImage == null) {
            selectedImage = selectedPhotos.first()
        }
    }

    FloodReportFormScreenContent(
        email = email,
        phoneNumber = phoneNumber,
        selectedPhotos = selectedPhotos,
        severity = severity,
        waterDepth = waterDepth,
        isRoadClosed = isRoadClosed,
        canAccessOffice = canAccessOffice,
        selectedImage = selectedImage,
        onEmailChange = { viewModel.updateEmail(it) },
        onPhoneNumberChange = { viewModel.updatePhoneNumber(it) },
        onSeverityChange = { viewModel.updateSeverity(it) },
        onWaterDepthChange = { viewModel.updateWaterDepth(it) },
        onRoadClosedChange = { viewModel.updateRoadClosed(it) },
        onOfficeAccessChange = { viewModel.updateOfficeAccess(it) },
        onImageRemove = { selectedImage = null },
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
    selectedPhotos: List<String>,
    severity: String,
    waterDepth: Double,
    isRoadClosed: Boolean?,
    canAccessOffice: Boolean?,
    selectedImage: String?,
    onEmailChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onSeverityChange: (String) -> Unit,
    onWaterDepthChange: (Double) -> Unit,
    onRoadClosedChange: (Boolean) -> Unit,
    onOfficeAccessChange: (Boolean) -> Unit,
    onImageRemove: () -> Unit,
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
                            imageVector = Icons.Default.ArrowBack,
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

            Spacer(modifier = Modifier.height(8.dp))

            // Your Recent Photos Section
            Text(
                text = "Your recent",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            // Photo Upload Box
            if (selectedImage != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Image
                            AsyncImage(
                                model = selectedImage,
                                contentDescription = "Selected photo",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "My recent",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = "2 MB",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }

                            // Close button
                            IconButton(
                                onClick = onImageRemove
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove photo"
                                )
                            }
                        }
                    }
                }
            } else {
                // Empty photo upload box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .border(
                            width = 1.dp,
                            color = Color.LightGray,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_camera),
                        contentDescription = "Upload photo",
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                }
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
                    Text(text = "Aff")
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
                    Text(text = "Ne")
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
        selectedPhotos = listOf(),
        severity = "medium",
        waterDepth = 12.0,
        isRoadClosed = false,
        canAccessOffice = true,
        selectedImage = null,
        onEmailChange = {},
        onPhoneNumberChange = {},
        onSeverityChange = {},
        onWaterDepthChange = {},
        onRoadClosedChange = {},
        onOfficeAccessChange = {},
        onImageRemove = {},
        onSubmit = {},
        onNavigateBack = {}
    )
}

