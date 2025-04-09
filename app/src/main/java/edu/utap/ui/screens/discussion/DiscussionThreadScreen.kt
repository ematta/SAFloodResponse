package edu.utap.ui.screens.discussion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.di.DiscussionViewModelFactory
import edu.utap.flood.model.DiscussionMessage
import edu.utap.ui.viewmodel.DiscussionViewModel


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
        if (threadId.isNotEmpty()) {
            viewModel.fetchThreadMessages(threadId)
        } else {
            viewModel.setError("Thread ID is empty")
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
        if (error != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Error: ${error ?: "Thread not found or has been deleted."}",
                    color = Color.Red
                )
                Button(onClick = onBackClick, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Back")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
}

/**
 * Card composable displaying a single discussion message.
 *
 * @param message The [DiscussionMessage] to display.
 */
@Composable
fun MessageCard(message: DiscussionMessage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Upvotes: ${message.upvotes}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
