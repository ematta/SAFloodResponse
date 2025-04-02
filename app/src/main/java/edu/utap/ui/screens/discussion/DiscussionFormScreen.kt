package edu.utap.ui.screens.discussion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utap.flood.di.DiscussionViewModelFactory
import edu.utap.ui.viewmodel.DiscussionViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var showPreview by remember { mutableStateOf(false) }

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
        }
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
                }

                // Submit Button
                Button(
                    onClick = {
                        viewModel.createNewDiscussion()
                        onNavigateBack()
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
