package edu.utap.ui.screens.discussion

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import edu.utap.di.DiscussionViewModelFactory
import edu.utap.models.DiscussionMessage
import edu.utap.models.FloodReport
import edu.utap.ui.viewmodel.DiscussionViewModel
import edu.utap.ui.viewmodel.FloodReportViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Discussion thread screen composable.
 *
 * Displays a list of messages in a discussion thread, allows composing and sending new messages,
 * and handles loading and error states.
 *
 * @param threadId The ID of the discussion thread.
 * @param viewModel The [DiscussionViewModel] managing thread data.
 * @param onBackClick Callback invoked when navigating back.
 */
@Composable
fun DiscussionThreadScreen(
    threadId: String,
    viewModel: DiscussionViewModel = viewModel(
        factory = DiscussionViewModelFactory.provideFactory(
            LocalContext.current
        )
    ),
    floodReportViewModel: FloodReportViewModel? = null,
    onBackClick: () -> Unit
) {
    val currentThread by viewModel.currentThread.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val newMessageText by viewModel.newMessageText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    
    // State for linked flood report
    var linkedReport by remember { mutableStateOf<FloodReport?>(null) }
    var loadingReport by remember { mutableStateOf(false) }
    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(threadId) {
        if (threadId.isNotEmpty()) {
            viewModel.fetchThreadMessages(threadId)
        } else {
            viewModel.setError("Thread ID is empty")
        }
    }
    
    // Load linked flood report if available
    LaunchedEffect(currentThread) {
        if (currentThread != null && currentThread?.reportId?.isNotEmpty() == true && floodReportViewModel != null) {
            loadingReport = true
            scope.launch {
                val reportResult = floodReportViewModel.floodReportRepository.getReportById(currentThread!!.reportId)
                reportResult.fold(
                    onSuccess = { report ->
                        linkedReport = report
                        loadingReport = false
                    },
                    onFailure = {
                        loadingReport = false
                    }
                )
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
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Close"
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion Thread") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && messages.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null && messages.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    // Display linked flood report info if available
                    if (linkedReport != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Linked Flood Report",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = linkedReport?.description ?: "No description",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    Text(
                                        text = "Severity: ${linkedReport?.severity}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    
                                    Text(
                                        text = "Water Depth: ${linkedReport?.waterDepthInches} inches",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    
                                    // Display linked flood report photos
                                    if (!linkedReport?.photoUrls.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text(
                                            text = "Photos:",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(linkedReport?.photoUrls ?: emptyList()) { url ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(100.dp)
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
                                        }
                                    }
                                }
                            }
                        }
                    } else if (loadingReport) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    // Display divider between report and messages
                    item {
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }

                    // Display thread description if available
                    if (!currentThread?.description.isNullOrEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = currentThread?.title ?: "",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = currentThread?.description ?: "",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    
                    // Messages list
                    items(messages) { message ->
                        MessageCard(
                            message = message,
                            viewModel = viewModel,
                            isAdmin = isAdmin,
                            threadId = threadId
                        )
                    }
                }

                // Message composer
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newMessageText,
                            onValueChange = viewModel::updateNewMessageText,
                            label = { Text("Type your message") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = { viewModel.submitMessage() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card composable displaying a single discussion message.
 *
 * @param message The [DiscussionMessage] to display.
 * @param viewModel The [DiscussionViewModel] for handling message operations.
 * @param isAdmin Boolean indicating if current user has admin role.
 * @param threadId The ID of the current thread.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MessageCard(
    message: DiscussionMessage,
    viewModel: DiscussionViewModel? = null,
    isAdmin: Boolean = false,
    threadId: String? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Message") },
            text = { Text("Are you sure you want to delete this message?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isAdmin && viewModel != null && threadId != null) {
                            viewModel.deleteMessage(threadId, message.messageId)
                        }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (isAdmin) Modifier.padding(8.dp) else Modifier
            )
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    if (isAdmin) {
                        showDeleteDialog = true
                    }
                },
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .then(
                    if (isAdmin) Modifier.padding(8.dp) else Modifier
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isAdmin) Modifier.padding(8.dp) else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                
                // Only show delete button if user is admin and we're not in preview mode
                if (isAdmin && viewModel != null && threadId != null) {
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete message"
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Upvotes: ${message.upvotes}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// --------------------
// Compose Previews
// --------------------

// Sample data for previews
val sampleMessage = DiscussionMessage(
    text = "This is a sample message for preview.",
    upvotes = 42
)

val sampleMessages = listOf(
    DiscussionMessage(text = "Hello, this is the first message!", upvotes = 3),
    DiscussionMessage(text = "Here's another message in the thread.", upvotes = 5),
    DiscussionMessage(text = "Previewing Compose UI is awesome!", upvotes = 10)
)

// Preview for MessageCard
@Preview(showBackground = true, name = "MessageCardPreview")
@Composable
fun MessageCardPreview() {
    MessageCard(message = sampleMessage)
}

// Preview for DiscussionThreadScreen (UI only, no real ViewModel)
@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    showBackground = true,
    widthDp = 400,
    heightDp = 700,
    name = "DiscussionThreadScreenPreview"
)
@Composable
fun DiscussionThreadScreenPreview() {
    // Provide sample state for preview
    val messages = sampleMessages
    val newMessageText = "Type your message"
    val isLoading = false
    val error: String? = null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion Thread") },
                navigationIcon = {
                    IconButton(onClick = { /* no-op */ }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // Messages list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    items(messages) { message ->
                        MessageCard(message = message)
                    }
                }

                // Message composer
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newMessageText,
                            onValueChange = {},
                            label = { Text("Type your message") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = { /* no-op */ },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send")
                        }
                    }
                }
            }
        }
    }
}
