package edu.utap.ui.screens.discussion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.di.DiscussionViewModelFactory
import edu.utap.ui.viewmodel.DiscussionViewModel

import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import edu.utap.ui.viewmodel.DiscussionViewModel
import edu.utap.ui.viewmodel.FloodReportViewModel
import edu.utap.repository.DiscussionRepositoryInterface
import edu.utap.repository.FloodReportRepositoryInterface
import edu.utap.ui.viewmodel.AuthViewModelInterface
import edu.utap.utils.LocationUtils
import edu.utap.models.DiscussionThread
import edu.utap.models.DiscussionMessage
import edu.utap.models.FloodReport
import edu.utap.models.FirestoreUser
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
/**
 * Discussion creation form screen composable.
 *
 * Allows users to create a new discussion thread, link it to a flood report,
 * add categories and tags, attach files, and preview before submission.
 *
 * @param viewModel The [DiscussionViewModel] managing discussion data.
 * @param onNavigateBack Callback invoked when navigating back.
 */
@Composable
fun DiscussionFormScreen(
    viewModel: DiscussionViewModel = viewModel(
        factory = DiscussionViewModelFactory.provideFactory(LocalContext.current)
    ),
    onNavigateBack: () -> Unit
) {
    val title by viewModel.newDiscussionTitle.collectAsState()
    val message by viewModel.newDiscussionMessage.collectAsState()
    val category by viewModel.newDiscussionCategory.collectAsState()
    val tags by viewModel.newDiscussionTags.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val creationSuccess by viewModel.discussionCreationSuccess.collectAsState()
    var showPreview by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // FloodReportViewModel instance
    val floodReportViewModel: edu.utap.ui.viewmodel.FloodReportViewModel = viewModel()

    LaunchedEffect(Unit) {
        floodReportViewModel.refreshActiveFloodReports()
    }

    val activeFloodReports by floodReportViewModel.activeFloodReports.collectAsState()
    var selectedFloodReportId by remember { mutableStateOf<String?>(null) }
    var floodDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Discussion") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPreview = !showPreview },
                        enabled = title.isNotEmpty() && message.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = if (showPreview) Icons.Default.Edit else Icons.Default.ArrowDropDown,
                            contentDescription = if (showPreview) "Edit" else "Preview"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (showPreview) {
                // Preview Mode
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (category.isNotEmpty()) {
                            Text(
                                text = "Category: $category",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (tags.isNotEmpty()) {
                            Text(
                                text = "Tags: ${tags.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Divider()
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (selectedFloodReportId != null) {
                            val selectedReport = activeFloodReports.find { it.reportId == selectedFloodReportId }
                            if (selectedReport != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Linked Flood Report: ${selectedReport.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                // Edit Mode
                // Discussion Title
                OutlinedTextField(
                    value = title,
                    onValueChange = viewModel::updateNewDiscussionTitle,
                    label = { Text("Discussion Title*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = title.isEmpty(),
                    supportingText = {
                        if (title.isEmpty()) {
                            Text("Required field")
                        }
                    }
                )

                // Flood Report Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = activeFloodReports.find { it.reportId == selectedFloodReportId }?.description ?: "",
                        onValueChange = {},
                        label = { Text("Link to Flood Report (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { floodDropdownExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Flood Report"
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = floodDropdownExpanded,
                        onDismissRequest = { floodDropdownExpanded = false }
                    ) {
                        activeFloodReports.forEach { report ->
                            DropdownMenuItem(
                                text = { Text(report.description) },
                                onClick = {
                                    selectedFloodReportId = report.reportId
                                    floodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category Selection
                var expanded by remember { mutableStateOf(false) }
                val categories = listOf("General", "Flood Report", "Help Request", "Announcement")
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select category"
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    viewModel.updateNewDiscussionCategory(item)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Tags Input
                var tagInput by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Add Tag") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (tagInput.isNotBlank()) {
                                    viewModel.updateNewDiscussionTags(tags + tagInput)
                                    tagInput = ""
                                }
                            }
                        )
                    )
                    IconButton(
                        onClick = {
                            if (tagInput.isNotBlank()) {
                                viewModel.updateNewDiscussionTags(tags + tagInput)
                                tagInput = ""
                            }
                        },
                        enabled = tagInput.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add tag"
                        )
                    }
                }

                // Display selected tags
                if (tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            SuggestionChip(
                                onClick = {
                                    viewModel.updateNewDiscussionTags(tags - tag)
                                },
                                label = { Text(tag) }
                            )
                        }
                    }
                }

                // Detailed Message
                OutlinedTextField(
                    value = message,
                    onValueChange = viewModel::updateNewDiscussionMessage,
                    label = { Text("Detailed Description*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10,
                    isError = message.isEmpty(),
                    supportingText = {
                        if (message.isEmpty()) {
                            Text("Required field")
                        }
                    }
                )

                // Attachment Section
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Attachments (Optional)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { /* TODO: Implement attachment picker */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add attachment"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add File")
                        }
                    }
                
                    // Handle feedback and navigation based on creation status
                    LaunchedEffect(creationSuccess) {
                        when (creationSuccess) {
                            true -> {
                                snackbarHostState.showSnackbar(
                                    message = "Discussion created successfully!",
                                    duration = SnackbarDuration.Short
                                )
                                // Wait a moment for snackbar to be seen before navigating
                                kotlinx.coroutines.delay(1500)
                                onNavigateBack()
                                viewModel.resetDiscussionCreationStatus()
                            }
                            false -> {
                                snackbarHostState.showSnackbar(
                                    message = "Error creating discussion: ${error ?: "Unknown error"}",
                                    duration = SnackbarDuration.Long
                                )
                                viewModel.resetDiscussionCreationStatus()
                            }
                            null -> {
                                // Initial state, do nothing
                            }
                        }
                    }
                }

                // Submit Button
                Button(
                    onClick = {
                        viewModel.createNewDiscussion()
                        // Navigation is now handled by LaunchedEffect below
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = title.isNotEmpty() && message.isNotEmpty()
                ) {
                    Text("Create Discussion")
                }
            }
        }
    }
}

//
// Preview for DiscussionFormScreen using real ViewModels with fake dependencies
//

// Minimal fake DiscussionRepository for preview
class FakeDiscussionRepository : DiscussionRepositoryInterface {
    override suspend fun createThread(thread: DiscussionThread) = Result.success(thread)
    override suspend fun getThreadById(threadId: String) = Result.success(
        DiscussionThread(threadId = threadId, reportId = "", createdBy = "user1")
    )
    override suspend fun updateThread(thread: DiscussionThread) = Result.success(thread)
    override suspend fun deleteThread(threadId: String) = Result.success(Unit)
    override suspend fun addMessage(threadId: String, message: DiscussionMessage) = Result.success(message)
    override suspend fun updateMessage(threadId: String, message: DiscussionMessage) = Result.success(message)
    override suspend fun deleteMessage(threadId: String, messageId: String) = Result.success(Unit)
    override fun observeThreadMessages(threadId: String) = flowOf(listOf<DiscussionMessage>())
    override fun observeAllThreads() = flowOf(listOf(
        DiscussionThread(threadId = "1", reportId = "", createdBy = "user1", title = "Preview Thread", description = "A preview thread")
    ))
}

// Minimal fake FloodReportRepository for preview
class FakeFloodReportRepository : FloodReportRepositoryInterface {
    override suspend fun createReport(report: FloodReport) = Result.success(report)
    override suspend fun getReportById(id: String) = Result.success(
        FloodReport(reportId = id, userId = "user1", latitude = 0.0, longitude = 0.0, description = "Preview Flood Report")
    )
    override fun observeAllReports() = flowOf(listOf(
        FloodReport(reportId = "1", userId = "user1", latitude = 0.0, longitude = 0.0, description = "Preview Flood Report 1"),
        FloodReport(reportId = "2", userId = "user2", latitude = 1.0, longitude = 1.0, description = "Preview Flood Report 2")
    ))
    override suspend fun updateReport(report: FloodReport) = Result.success(report)
    override suspend fun deleteReport(id: String) = Result.success(Unit)
    override fun getReportsInRadius(latitude: Double, longitude: Double, radiusKm: Double) = flowOf(listOf<FloodReport>())
}

// Minimal fake AuthViewModelInterface for preview
class FakeAuthViewModel : AuthViewModelInterface {
    private val _authState = MutableStateFlow<edu.utap.auth.AuthState>(edu.utap.auth.AuthState.Idle.Unauthenticated)
    override val authState: StateFlow<edu.utap.auth.AuthState> = _authState
    override fun register(email: String, password: String, name: String, role: String, function: (Boolean, String?) -> Unit) { function(true, null) }
    override fun login(email: String, password: String, function: (Boolean, String?) -> Unit) { function(true, null) }
    override fun logout() = edu.utap.auth.AuthState.Idle.Unauthenticated
    override fun resetPassword(email: String, callback: (Boolean, String?) -> Unit) { callback(true, null) }
    override fun getCurrentUser(): FirestoreUser? = FirestoreUser(userId = "user1", name = "Preview User", email = "preview@example.com")
    override fun restoreAuthState() {}
    override fun updateAuthState(sent: edu.utap.auth.AuthState) {}
}

// Minimal fake LocationUtils for preview
class FakeLocationUtils : LocationUtils {
    override fun requestLocationUpdates(callback: (android.location.Location) -> Unit) {}
}

@Preview(
    name = "DiscussionFormScreen Preview",
    showBackground = true,
    group = "Discussion"
)
@Composable
fun DiscussionFormScreenPreview() {
    Log.d("DiscussionFormScreenPreview", "Preview composable invoked")
    val discussionViewModel = DiscussionViewModel(
        discussionRepository = FakeDiscussionRepository(),
        authViewModel = FakeAuthViewModel()
    )
    val floodReportViewModel = FloodReportViewModel(
        floodReportRepository = FakeFloodReportRepository(),
        authViewModel = FakeAuthViewModel(),
        locationUtils = FakeLocationUtils()
    )
    // Provide fake state for preview
    discussionViewModel.updateNewDiscussionTitle("Preview Discussion Title")
    discussionViewModel.updateNewDiscussionMessage("This is a preview of the discussion message.")
    discussionViewModel.updateNewDiscussionCategory("General")
    discussionViewModel.updateNewDiscussionTags(listOf("tag1", "tag2"))
    // Compose the screen
    DiscussionFormScreen(
        viewModel = discussionViewModel,
        onNavigateBack = { Log.d("DiscussionFormScreenPreview", "Navigate back (preview)") }
    )
}

// Minimal fake DiscussionViewModel for preview
class DiscussionFormFakeDiscussionViewModel : DiscussionViewModel() {
    private val _newDiscussionTitle = MutableStateFlow("Preview Discussion Title")
    override val newDiscussionTitle: StateFlow<String> = _newDiscussionTitle

    private val _newDiscussionMessage = MutableStateFlow("This is a preview of the discussion message.")
    override val newDiscussionMessage: StateFlow<String> = _newDiscussionMessage

    private val _newDiscussionCategory = MutableStateFlow("General")
    override val newDiscussionCategory: StateFlow<String> = _newDiscussionCategory

    private val _newDiscussionTags = MutableStateFlow(listOf("tag1", "tag2"))
    override val newDiscussionTags: StateFlow<List<String>> = _newDiscussionTags

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error

    private val _discussionCreationSuccess = MutableStateFlow<Boolean?>(null)
    override val discussionCreationSuccess: StateFlow<Boolean?> = _discussionCreationSuccess

    // Override update methods as no-ops for preview
    override fun updateNewDiscussionTitle(title: String) {}
    override fun updateNewDiscussionMessage(message: String) {}
    override fun updateNewDiscussionCategory(category: String) {}
    override fun updateNewDiscussionTags(tags: List<String>) {}
    override fun createNewDiscussion() {}
    override fun resetDiscussionCreationStatus() {}
}

// Minimal fake FloodReportViewModel for preview
class DiscussionFormFakeFloodReportViewModel : FloodReportViewModel() {
    private val _activeFloodReports = MutableStateFlow(
        listOf(
            object {
                val reportId = "1"
                val description = "Flood Report 1"
            },
            object {
                val reportId = "2"
                val description = "Flood Report 2"
            }
        )
    )
    override val activeFloodReports: StateFlow<List<Any>> = _activeFloodReports

    override fun refreshActiveFloodReports() {}
}

@Preview(
    name = "DiscussionFormScreen Preview",
    showBackground = true,
    group = "Discussion"
)
@Composable
fun DiscussionFormScreenPreview() {
    Log.d("DiscussionFormScreenPreview", "Preview composable invoked")
    // Provide fake ViewModels and a no-op lambda
    val fakeDiscussionViewModel = DiscussionFormFakeDiscussionViewModel()
    val fakeFloodReportViewModel = DiscussionFormFakeFloodReportViewModel()
    // Use CompositionLocalProvider if needed to inject fakeFloodReportViewModel
    DiscussionFormScreen(
        viewModel = fakeDiscussionViewModel,
        onNavigateBack = { Log.d("DiscussionFormScreenPreview", "Navigate back (preview)") }
    )
}
