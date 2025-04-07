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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.flood.di.DiscussionViewModelFactory
import edu.utap.flood.model.DiscussionThread
import edu.utap.ui.components.AppHeader
import edu.utap.ui.viewmodel.DiscussionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionListScreen(
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion Threads") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onThreadClick("new") },
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
                        text = error.toString(),
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
                            onClick = { onThreadClick(thread.threadId) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadCard(
    thread: DiscussionThread,
    onClick: () -> Unit
) {
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
private fun formatTimestamp(date: Date): String {
    return try {
        val sdf = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        sdf.format(date)
    } catch (e: Exception) {
        ""
    }
}
