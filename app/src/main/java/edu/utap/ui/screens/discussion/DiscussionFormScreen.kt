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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.ui.theme.SAFloodResponseTheme
import edu.utap.ui.viewmodel.DiscussionViewModel
import edu.utap.ui.viewmodel.FloodReportViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiscussionFormScreen(
    viewModel: DiscussionViewModel = viewModel(
        factory = edu.utap.di.DiscussionViewModelFactory.provideFactory(
            androidx.compose.ui.platform.LocalContext.current
        )
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

    // FloodReportViewModel instance
    val floodReportViewModel: FloodReportViewModel = viewModel()
    LaunchedEffect(Unit) {
        floodReportViewModel.refreshActiveFloodReports()
    }
    val activeFloodReports by floodReportViewModel.activeFloodReports.collectAsState()
    var selectedFloodReportId by remember { mutableStateOf<String?>(null) }
    var floodDropdownExpanded by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val categories = listOf("General", "Flood Report", "Help Request", "Announcement")

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
                            imageVector = if (showPreview) {
                                Icons.Default.Edit
                            } else {
                                Icons.Default.ArrowDropDown
                            },
                            contentDescription = if (showPreview) "Edit" else "Preview"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        DiscussionFormScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            title = title,
            onTitleChange = viewModel::updateNewDiscussionTitle,
            message = message,
            onMessageChange = viewModel::updateNewDiscussionMessage,
            category = category,
            onCategoryChange = viewModel::updateNewDiscussionCategory,
            categories = categories,
            categoryDropdownExpanded = categoryDropdownExpanded,
            onCategoryDropdownExpandedChange = { categoryDropdownExpanded = it },
            tags = tags,
            tagInput = tagInput,
            onTagInputChange = { tagInput = it },
            onAddTag = {
                if (it.isNotBlank()) {
                    viewModel.updateNewDiscussionTags(tags + it)
                }
            },
            onRemoveTag = { tag -> viewModel.updateNewDiscussionTags(tags - tag) },
            isLoading = isLoading,
            error = error,
            showPreview = showPreview,
            onShowPreviewChange = { showPreview = it },
            floodReports = activeFloodReports.map {
                SimpleFloodReport(it.reportId, it.description)
            },
            selectedFloodReportId = selectedFloodReportId,
            onFloodReportSelect = { selectedFloodReportId = it },
            floodDropdownExpanded = floodDropdownExpanded,
            onFloodDropdownExpandedChange = { floodDropdownExpanded = it },
            onSubmit = {
                viewModel.createNewDiscussion()
            },
            onNavigateBack = onNavigateBack
        )

        // Handle feedback and navigation based on creation status
        LaunchedEffect(creationSuccess) {
            when (creationSuccess) {
                true -> {
                    snackbarHostState.showSnackbar(
                        message = "Discussion created successfully!",
                        duration = SnackbarDuration.Short
                    )
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
}

// Minimal data class for preview and stateless UI
data class SimpleFloodReport(val reportId: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiscussionFormScreenContent(
    modifier: Modifier = Modifier,
    title: String,
    onTitleChange: (String) -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    categories: List<String>,
    categoryDropdownExpanded: Boolean,
    onCategoryDropdownExpandedChange: (Boolean) -> Unit,
    tags: List<String>,
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    showPreview: Boolean,
    onShowPreviewChange: (Boolean) -> Unit,
    floodReports: List<SimpleFloodReport>,
    selectedFloodReportId: String?,
    onFloodReportSelect: (String?) -> Unit,
    floodDropdownExpanded: Boolean,
    onFloodDropdownExpandedChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = modifier,
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
                        val selectedReport = floodReports.find {
                            it.reportId ==
                                selectedFloodReportId
                        }
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
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
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
                    value =
                    floodReports.find { it.reportId == selectedFloodReportId }?.description
                        ?: "",
                    onValueChange = {},
                    label = { Text("Link to Flood Report (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { onFloodDropdownExpandedChange(true) }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Flood Report"
                            )
                        }
                    }
                )
                DropdownMenu(
                    expanded = floodDropdownExpanded,
                    onDismissRequest = { onFloodDropdownExpandedChange(false) }
                ) {
                    floodReports.forEach { report ->
                        DropdownMenuItem(
                            text = { Text(report.description) },
                            onClick = {
                                onFloodReportSelect(report.reportId)
                                onFloodDropdownExpandedChange(false)
                            }
                        )
                    }
                }
            }

            // Category Selection
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { onCategoryDropdownExpandedChange(true) }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select category"
                            )
                        }
                    }
                )
                DropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { onCategoryDropdownExpandedChange(false) }
                ) {
                    categories.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                onCategoryChange(item)
                                onCategoryDropdownExpandedChange(false)
                            }
                        )
                    }
                }
            }

            // Tags Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = onTagInputChange,
                    label = { Text("Add Tag") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (tagInput.isNotBlank()) {
                                onAddTag(tagInput)
                                onTagInputChange("")
                            }
                        }
                    )
                )
                IconButton(
                    onClick = {
                        if (tagInput.isNotBlank()) {
                            onAddTag(tagInput)
                            onTagInputChange("")
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
                            onClick = { onRemoveTag(tag) },
                            label = { Text(tag) }
                        )
                    }
                }
            }

            // Detailed Message
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
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

            // Attachment Section (stubbed for preview)
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
                        onClick = { /* No-op in preview/stateless */ },
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
            }

            // Submit Button
            Button(
                onClick = onSubmit,
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

@Preview(name = "Discussion Form Light", showBackground = true)
@Composable
fun PreviewDiscussionFormScreenContent_Light() {
    SAFloodResponseTheme(darkTheme = false) {
        var title by remember { mutableStateOf("Sample Title") }
        var message by remember { mutableStateOf("This is a sample discussion message.") }
        var category by remember { mutableStateOf("General") }
        var tags by remember { mutableStateOf(listOf("tag1", "tag2")) }
        var tagInput by remember { mutableStateOf("") }
        var showPreview by remember { mutableStateOf(false) }
        var selectedFloodReportId by remember { mutableStateOf<String?>(null) }
        var floodDropdownExpanded by remember { mutableStateOf(false) }
        var categoryDropdownExpanded by remember { mutableStateOf(false) }
        val categories = listOf("General", "Flood Report", "Help Request", "Announcement")
        val floodReports = listOf(
            SimpleFloodReport("1", "Flood at Main St."),
            SimpleFloodReport("2", "Flood at River Rd.")
        )

        DiscussionFormScreenContent(
            title = title,
            onTitleChange = { title = it },
            message = message,
            onMessageChange = { message = it },
            category = category,
            onCategoryChange = { category = it },
            categories = categories,
            categoryDropdownExpanded = categoryDropdownExpanded,
            onCategoryDropdownExpandedChange = { categoryDropdownExpanded = it },
            tags = tags,
            tagInput = tagInput,
            onTagInputChange = { tagInput = it },
            onAddTag = { if (it.isNotBlank()) tags = tags + it },
            onRemoveTag = { tags = tags - it },
            isLoading = false,
            error = null,
            showPreview = showPreview,
            onShowPreviewChange = { showPreview = it },
            floodReports = floodReports,
            selectedFloodReportId = selectedFloodReportId,
            onFloodReportSelect = { selectedFloodReportId = it },
            floodDropdownExpanded = floodDropdownExpanded,
            onFloodDropdownExpandedChange = { floodDropdownExpanded = it },
            onSubmit = {},
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "Discussion Form Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PreviewDiscussionFormScreenContent_Dark() {
    SAFloodResponseTheme(darkTheme = true) {
        var title by remember { mutableStateOf("Sample Title") }
        var message by remember { mutableStateOf("This is a sample discussion message.") }
        var category by remember { mutableStateOf("General") }
        var tags by remember { mutableStateOf(listOf("tag1", "tag2")) }
        var tagInput by remember { mutableStateOf("") }
        var showPreview by remember { mutableStateOf(false) }
        var selectedFloodReportId by remember { mutableStateOf<String?>(null) }
        var floodDropdownExpanded by remember { mutableStateOf(false) }
        var categoryDropdownExpanded by remember { mutableStateOf(false) }
        val categories = listOf("General", "Flood Report", "Help Request", "Announcement")
        val floodReports = listOf(
            SimpleFloodReport("1", "Flood at Main St."),
            SimpleFloodReport("2", "Flood at River Rd.")
        )

        DiscussionFormScreenContent(
            title = title,
            onTitleChange = { title = it },
            message = message,
            onMessageChange = { message = it },
            category = category,
            onCategoryChange = { category = it },
            categories = categories,
            categoryDropdownExpanded = categoryDropdownExpanded,
            onCategoryDropdownExpandedChange = { categoryDropdownExpanded = it },
            tags = tags,
            tagInput = tagInput,
            onTagInputChange = { tagInput = it },
            onAddTag = { if (it.isNotBlank()) tags = tags + it },
            onRemoveTag = { tags = tags - it },
            isLoading = false,
            error = null,
            showPreview = showPreview,
            onShowPreviewChange = { showPreview = it },
            floodReports = floodReports,
            selectedFloodReportId = selectedFloodReportId,
            onFloodReportSelect = { selectedFloodReportId = it },
            floodDropdownExpanded = floodDropdownExpanded,
            onFloodDropdownExpandedChange = { floodDropdownExpanded = it },
            onSubmit = {},
            onNavigateBack = {}
        )
    }
}
