package edu.utap.ui.screens.flood

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import edu.utap.models.FloodReport
import edu.utap.repository.FloodReportRepository
import edu.utap.ui.navigation.AuthenticatedRoutes
import edu.utap.ui.viewmodel.FloodReportViewModel
import edu.utap.utils.RoleUtils
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloodReportDetailScreen(
    reportId: String,
    viewModel: FloodReportViewModel,
    onEdit: (FloodReport) -> Unit = {},
    navController: NavHostController,
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<FloodReport?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentUser = viewModel.authViewModel.getCurrentUser()

    LaunchedEffect(reportId) {
        loading = true
        error = null
        scope.launch {
            val result = viewModel.floodReportRepository.getReportById(reportId)
            result.fold(
                onSuccess = {
                    report = it
                    loading = false
                },
                onFailure = {
                    error = it.message
                    loading = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flood Report Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                loading -> CircularProgressIndicator()
                error != null -> Text(error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                report != null -> {
                    val canEdit = currentUser != null &&
                        (currentUser.userId == report!!.userId || currentUser.role == RoleUtils.ROLE_ADMIN)
                    FloodReportDetailContent(
                        report = report!!,
                        canEdit = canEdit,
                        onEdit = onEdit,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun FloodReportDetailContent(
    report: FloodReport,
    canEdit: Boolean,
    onEdit: (FloodReport) -> Unit,
    viewModel: FloodReportViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }

    // Camera launcher for taking new photos
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            uploading = true
            uploadError = null
            viewModel.uploadFloodReportPhoto(context, cameraImageUri!!, report.reportId) { result ->
                uploading = false
                result.onFailure { uploadError = it.message }
            }
        }
    }

    // Gallery launcher for selecting photos from gallery
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploading = true
            uploadError = null
            viewModel.uploadFloodReportPhoto(context, uri, report.reportId) { result ->
                uploading = false
                result.onFailure { uploadError = it.message }
            }
        }
    }

    // Photo preview dialog
    if (selectedPhotoUrl != null) {
        Dialog(onDismissRequest = { selectedPhotoUrl = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = selectedPhotoUrl,
                        contentDescription = "Enlarged photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { selectedPhotoUrl = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Close"
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (report.description.isNotEmpty()) report.description else "No description provided",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Location: ${report.latitude}, ${report.longitude}", 
                     style = MaterialTheme.typography.bodyMedium)
                Text("Severity: ${report.severity}", 
                     style = MaterialTheme.typography.bodyMedium)
                Text("Water Depth: ${report.waterDepthInches} inches", 
                     style = MaterialTheme.typography.bodyMedium)
                Text("Road Status: ${if (report.isRoadClosed) "Closed" else "Open"}", 
                     style = MaterialTheme.typography.bodyMedium)
                Text("Status: ${report.status}", 
                     style = MaterialTheme.typography.bodyMedium)
            }
        }
        
        // Photos section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Photos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (report.photoUrls.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(report.photoUrls) { url ->
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray)
                                    .clickable { selectedPhotoUrl = url }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Flood photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        
                        // Only show add photo buttons if user can edit
                        if (canEdit) {
                            // Camera button
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE6E9FF))
                                        .clickable {
                                            val photoFile = File(
                                                context.cacheDir,
                                                "flood_photo_${System.currentTimeMillis()}.jpg"
                                            )
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                context.packageName + ".provider",
                                                photoFile
                                            )
                                            cameraImageUri = uri
                                            launcher.launch(uri)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Take photo",
                                        tint = Color(0xFF8C9EFF),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            
                            // Gallery button
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE6E9FF))
                                        .clickable {
                                            galleryLauncher.launch("image/*")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Choose from gallery",
                                        tint = Color(0xFF8C9EFF),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (canEdit) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val photoFile = File(
                                            context.cacheDir,
                                            "flood_photo_${System.currentTimeMillis()}.jpg"
                                        )
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            context.packageName + ".provider",
                                            photoFile
                                        )
                                        cameraImageUri = uri
                                        launcher.launch(uri)
                                    },
                                    enabled = !uploading
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Take photo"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Take Photo")
                                }
                                
                                Button(
                                    onClick = {
                                        galleryLauncher.launch("image/*")
                                    },
                                    enabled = !uploading
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Choose from gallery"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Gallery")
                                }
                            }
                        } else {
                            Text("No photos available", color = Color.Gray)
                        }
                    }
                }
                
                if (uploading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text("Uploading photo...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                if (uploadError != null) {
                    Text(
                        text = "Error: $uploadError",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        // Discussion thread section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Discussion Thread",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        // Navigate to discussion thread
                        navController.navigate(
                            AuthenticatedRoutes.DISCUSSIONS_THREAD.replace(
                                "{threadId}",
                                report.reportId
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Discussion")
                }
            }
        }
        
        // Other actions
        if (canEdit) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { onEdit(report) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit Report")
                    }
                }
            }
        }
    }
}
