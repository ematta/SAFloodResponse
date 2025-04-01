package edu.utap.ui.screens.discussion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.flood.di.DiscussionViewModelFactory
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Text("Loading threads...")
            } else if (error != null) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            } else {
                LazyColumn {
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
    thread: edu.utap.flood.model.DiscussionThread,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Thread for report: ${thread.reportId}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Messages: ${thread.messages.size}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
