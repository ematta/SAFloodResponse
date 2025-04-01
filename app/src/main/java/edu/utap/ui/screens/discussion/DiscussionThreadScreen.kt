package edu.utap.ui.screens.discussion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.flood.di.DiscussionViewModelFactory
import edu.utap.ui.components.AppHeader
import edu.utap.ui.viewmodel.DiscussionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionThreadScreen(
    threadId: String,
    viewModel: DiscussionViewModel = viewModel(factory = DiscussionViewModelFactory.provideFactory(
        LocalContext.current
    )),
    onBackClick: () -> Unit
) {
    val currentThread by viewModel.currentThread.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val newMessageText by viewModel.newMessageText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(threadId) {
        viewModel.fetchThreadMessages(threadId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion") },
                navigationIcon = {
                    Button(onClick = onBackClick) {
                        Text("Back")
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
            if (isLoading) {
                Text("Loading messages...")
            } else if (error != null) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(messages) { message ->
                        MessageCard(message = message)
                    }
                }

                // Message composer
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    TextField(
                        value = newMessageText,
                        onValueChange = viewModel::updateNewMessageText,
                        label = { Text("Type your message") },
                        modifier = Modifier.fillMaxSize()
                    )
                    Button(
                        onClick = {
                            viewModel.submitMessage()
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(message: edu.utap.flood.model.DiscussionMessage) {
    Card(
        modifier = Modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Upvotes: ${message.upvotes}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
