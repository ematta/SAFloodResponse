package edu.utap.ui.screens.discussion

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import edu.utap.di.DiscussionViewModelFactory
import edu.utap.models.DiscussionThread
import edu.utap.ui.viewmodel.DiscussionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Discussion list screen composable.
 *
 * Displays a list of discussion threads, with loading and error states,
 * and a button to create a new discussion.
 *
 * @param navController Navigation controller for navigating to thread details or new thread.
 * @param viewModel The [DiscussionViewModel] managing discussion data.
 * @param onThreadClick Callback invoked when a thread is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionListScreen(
    navController: androidx.navigation.NavController,
    viewModel: DiscussionViewModel = viewModel(
        factory = DiscussionViewModelFactory.provideFactory(LocalContext.current)
    ),
    onThreadClick: (String) -> Unit
) {
    val allThreads by viewModel.allThreads.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchAllThreads()
    }

    DiscussionListScreenContent(
        isLoading = isLoading,
        error = error,
        allThreads = allThreads,
        onThreadClick = { thread -> onThreadClick(thread.threadId) },
        onAddClick = { navController.navigate("discussions/new") }
    )
}

/**
 * Stateless UI content for the discussion list screen.
 * This is previewable with sample/mock data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionListScreenContent(
    isLoading: Boolean,
    error: String?,
    allThreads: List<DiscussionThread>,
    onThreadClick: (DiscussionThread) -> Unit,
    onAddClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion Threads") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Discussion"
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (error != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error loading discussions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (allThreads.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = edu.utap.R.drawable.ic_smiley_face),
                        contentDescription = "No topics",
                        modifier = Modifier
                            .height(100.dp)
                            .padding(bottom = 16.dp)
                    )
                    Text(
                        text = "No topics yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Be the first to start a discussion!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    items(allThreads) { thread ->
                        ThreadCard(
                            thread = thread,
                            onClick = { onThreadClick(thread) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card composable displaying a single discussion thread summary.
 *
 * @param thread The [DiscussionThread] to display.
 * @param onClick Callback invoked when the card is clicked.
 */
/**
 * Card composable displaying a single discussion thread summary.
 *
 * @param thread The [DiscussionThread] to display.
 * @param onClick Callback invoked when the card is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadCard(thread: DiscussionThread, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = thread.reportId.ifEmpty { thread.category.ifEmpty { "Discussion" } },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            val displayTimestamp = thread.editTimestamp ?: thread.timestamp
            if (displayTimestamp != null) {
                Text(
                    text = formatTimestamp(displayTimestamp.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Messages: ${thread.messages.size}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (thread.tags.isNotEmpty()) {
                    Text(
                        text = thread.tags.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
private fun formatTimestamp(date: Date): String = try {
    val sdf = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    sdf.format(date)
} catch (e: Exception) {
    ""
}

// --- Previews and sample data ---

// Sample data provider for DiscussionThread
class DiscussionThreadPreviewParameterProvider : PreviewParameterProvider<DiscussionThread> {
    override val values: Sequence<DiscussionThread> = sequenceOf(
        DiscussionThread(
            threadId = "1",
            reportId = "RPT-123",
            category = "Flood",
            messages = listOf(),
            tags = listOf("urgent", "weather"),
            timestamp = Timestamp.now(),
            editTimestamp = null
        ),
        DiscussionThread(
            threadId = "2",
            reportId = "",
            category = "General",
            messages = listOf(),
            tags = emptyList(),
            timestamp = Timestamp.now(),
            editTimestamp = null
        )
    )
}

// Preview for ThreadCard
@Preview(showBackground = true, name = "ThreadCard Preview")
@Composable
fun ThreadCardPreview(
    @PreviewParameter(DiscussionThreadPreviewParameterProvider::class) thread: DiscussionThread
) {
    ThreadCard(thread = thread, onClick = {})
}

// Preview for DiscussionListScreenContent with sample data
@Preview(showBackground = true, widthDp = 400, name = "DiscussionListScreenContent Preview")
@Composable
fun DiscussionListScreenContentPreview() {
    val sampleThreads = listOf(
        DiscussionThread(
            threadId = "1",
            reportId = "RPT-123",
            category = "Flood",
            messages = listOf(),
            tags = listOf("urgent", "weather"),
            timestamp = Timestamp.now(),
            editTimestamp = null
        ),
        DiscussionThread(
            threadId = "2",
            reportId = "",
            category = "General",
            messages = listOf(),
            tags = emptyList(),
            timestamp = Timestamp.now(),
            editTimestamp = null
        )
    )
    DiscussionListScreenContent(
        isLoading = false,
        error = null,
        allThreads = sampleThreads,
        onThreadClick = {},
        onAddClick = {}
    )
}

// Preview for DiscussionListScreenContent loading state
@Preview(showBackground = true, name = "DiscussionListScreenContent Loading")
@Composable
fun DiscussionListScreenContentLoadingPreview() {
    DiscussionListScreenContent(
        isLoading = true,
        error = null,
        allThreads = emptyList(),
        onThreadClick = {},
        onAddClick = {}
    )
}

// Preview for DiscussionListScreenContent error state
@Preview(showBackground = true, name = "DiscussionListScreenContent Error")
@Composable
fun DiscussionListScreenContentErrorPreview() {
    DiscussionListScreenContent(
        isLoading = false,
        error = "Network error",
        allThreads = emptyList(),
        onThreadClick = {},
        onAddClick = {}
    )
}

// Preview for DiscussionListScreenContent empty state
@Preview(showBackground = true, name = "DiscussionListScreenContent Empty")
@Composable
fun DiscussionListScreenContentEmptyPreview() {
    DiscussionListScreenContent(
        isLoading = false,
        error = null,
        allThreads = emptyList(),
        onThreadClick = {},
        onAddClick = {}
    )
}
