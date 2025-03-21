package edu.utap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.utap.models.DiscussionMessage
import edu.utap.models.DiscussionThread
import edu.utap.repository.DiscussionRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for managing discussion threads and messages.
 *
 * Handles:
 * - Fetching all threads and messages
 * - Creating new threads and discussions
 * - Composing and submitting messages
 * - Upvoting messages
 * - Managing UI state such as loading, errors, and form inputs
 *
 * @param discussionRepository The repository for discussion data.
 * @param authViewModel The authentication ViewModel interface.
 */
class DiscussionViewModel(
    private val discussionRepository: DiscussionRepositoryInterface,
    private val authViewModel: AuthViewModelInterface
) : ViewModel() {
    // Current thread state
    private val _currentThread = MutableStateFlow<DiscussionThread?>(null)
    val currentThread: StateFlow<DiscussionThread?> = _currentThread

    // Messages in current thread
    private val _messages = MutableStateFlow<List<DiscussionMessage>>(emptyList())
    val messages: StateFlow<List<DiscussionMessage>> = _messages

    // All available threads
    private val _allThreads = MutableStateFlow<List<DiscussionThread>>(emptyList())
    val allThreads: StateFlow<List<DiscussionThread>> = _allThreads

    // New message composition
    private val _newMessageText = MutableStateFlow("")
    val newMessageText: StateFlow<String> = _newMessageText

    // New discussion composition
    private val _newDiscussionTitle = MutableStateFlow("")
    val newDiscussionTitle: StateFlow<String> = _newDiscussionTitle

    private val _newDiscussionMessage = MutableStateFlow("")
    val newDiscussionMessage: StateFlow<String> = _newDiscussionMessage

    // New discussion category
    private val _newDiscussionCategory = MutableStateFlow("")
    val newDiscussionCategory: StateFlow<String> = _newDiscussionCategory

    // New discussion tags
    private val _newDiscussionTags = MutableStateFlow<List<String>>(emptyList())
    val newDiscussionTags: StateFlow<List<String>> = _newDiscussionTags

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // State to track discussion creation result
    private val _discussionCreationSuccess = MutableStateFlow<Boolean?>(null)
    val discussionCreationSuccess: StateFlow<Boolean?> = _discussionCreationSuccess

    // State to track if user is admin
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    fun setError(message: String) {
        _error.value = message
    }

    // Initialize by loading all threads and checking admin status
    init {
        fetchAllThreads()
        checkAdminStatus()
    }

    /**
     * Checks if the current user has admin role and updates the isAdmin state.
     */
    private fun checkAdminStatus() {
        viewModelScope.launch {
            val user = authViewModel.getCurrentUser()
            _isAdmin.value = user != null && authViewModel.hasPermission("admin")
        }
    }

    fun updateNewMessageText(text: String) {
        _newMessageText.value = text
    }

    fun updateNewDiscussionTitle(title: String) {
        _newDiscussionTitle.value = title
    }

    fun updateNewDiscussionMessage(message: String) {
        _newDiscussionMessage.value = message
    }

    fun updateNewDiscussionCategory(category: String) {
        _newDiscussionCategory.value = category
    }

    fun updateNewDiscussionTags(tags: List<String>) {
        _newDiscussionTags.value = tags
    }

    fun fetchAllThreads() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                discussionRepository.observeAllThreads()
                    .collect { threads ->
                        _allThreads.value = threads
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load discussion threads"
                _isLoading.value = false
            }
        }
    }

    fun fetchThreadMessages(threadId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Get thread details
                val threadResult = discussionRepository.getThreadById(threadId)
                threadResult.fold(
                    onSuccess = { thread ->
                        _currentThread.value = thread
                    },
                    onFailure = { error ->
                        _error.value = error.message ?: "Failed to load thread"
                    }
                )

                // Get messages
                discussionRepository.observeThreadMessages(threadId)
                    .collect { messages ->
                        _messages.value = messages
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load messages"
                _isLoading.value = false
            }
        }
    }

    fun createNewThread(reportId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val currentUser = authViewModel.getCurrentUser()
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    return@launch
                }

                val newThread = DiscussionThread(
                    threadId = reportId, // Using reportId as threadId for 1:1 mapping
                    reportId = reportId,
                    createdBy = currentUser.userId
                )

                val result = discussionRepository.createThread(newThread)
                result.fold(
                    onSuccess = { thread ->
                        _currentThread.value = thread
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _error.value = error.message ?: "Failed to create thread"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create thread"
                _isLoading.value = false
            }
        }
    }

    fun createNewDiscussion() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _discussionCreationSuccess.value = null // Reset status at the start

            try {
                val currentUser = authViewModel.getCurrentUser()
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    _discussionCreationSuccess.value = false // Signal failure
                    return@launch
                }

                if (_newDiscussionTitle.value.isBlank()) {
                    _error.value = "Title cannot be empty"
                    _isLoading.value = false
                    _discussionCreationSuccess.value = false // Signal failure
                    return@launch
                }

                if (_newDiscussionMessage.value.isBlank()) {
                    _error.value = "Message cannot be empty"
                    _isLoading.value = false
                    _discussionCreationSuccess.value = false // Signal failure
                    return@launch
                }

                // Generate a unique ID for the new thread using UUID
                val threadId = UUID.randomUUID().toString()

                val newThread = DiscussionThread(
                    threadId = threadId,
                    reportId = "", // No associated flood report
                    createdBy = currentUser.userId,
                    category = _newDiscussionCategory.value,
                    tags = _newDiscussionTags.value
                )

                // Generate a unique ID for the new message using UUID
                val newMessage = DiscussionMessage(
                    messageId = UUID.randomUUID().toString(),
                    userId = currentUser.userId,
                    text = _newDiscussionMessage.value
                )

                // First create the thread
                val threadResult = discussionRepository.createThread(newThread)
                threadResult.fold(
                    onSuccess = { thread ->
                        // Then add the initial message
                        val messageResult = discussionRepository.addMessage(
                            thread.threadId,
                            newMessage
                        )
                        messageResult.fold(
                            onSuccess = {
                                _newDiscussionTitle.value = ""
                                _newDiscussionMessage.value = ""
                                _newDiscussionCategory.value = "" // Clear category
                                _newDiscussionTags.value = emptyList() // Clear tags
                                _isLoading.value = false
                                _discussionCreationSuccess.value = true // Signal success
                                fetchAllThreads() // Refresh the list
                            },
                            onFailure = { error ->
                                android.util.Log.e(
                                    "DiscussionViewModel",
                                    "Failed to add initial message for thread ${thread.threadId}",
                                    error
                                )
                                _error.value = "Failed to add initial message: ${error.localizedMessage ?: "Unknown error"}"
                                _isLoading.value = false
                                _discussionCreationSuccess.value = false // Signal failure
                            }
                        )
                    },
                    onFailure = { error ->
                        android.util.Log.e("DiscussionViewModel", "Failed to create thread", error)
                        _error.value = "Failed to create thread: ${error.localizedMessage ?: "Unknown error"}"
                        _isLoading.value = false
                        _discussionCreationSuccess.value = false // Signal failure
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("DiscussionViewModel", "Failed to create discussion", e)
                _error.value = "Failed to create discussion: ${e.localizedMessage ?: "Unknown error"}"
                _isLoading.value = false
                _discussionCreationSuccess.value = false // Signal failure
                _isLoading.value = false
            }
        }
    }

    fun createNewDiscussion(title: String, description: String, reportId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _discussionCreationSuccess.value = null

            try {
                val currentUser = authViewModel.getCurrentUser()
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    _discussionCreationSuccess.value = false
                    return@launch
                }

                // Generate a unique ID for the new thread using UUID
                val threadId = UUID.randomUUID().toString()
                // val timestamp = System.currentTimeMillis() // Keep timestamp if needed for other purposes, but not ID

                val newThread = DiscussionThread(
                    threadId = threadId,
                    reportId = reportId,
                    createdBy = currentUser.userId,
                    title = title,
                    description = description
                )

                val threadResult = discussionRepository.createThread(newThread)
                threadResult.fold(
                    onSuccess = { thread ->
                        _isLoading.value = false
                        _discussionCreationSuccess.value = true
                        fetchAllThreads()
                    },
                    onFailure = { error ->
                        _error.value = error.message ?: "Failed to create discussion thread"
                        _isLoading.value = false
                        _discussionCreationSuccess.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create discussion thread"
                _isLoading.value = false
                _discussionCreationSuccess.value = false
            }
        }
    }

    fun submitMessage() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val currentUser = authViewModel.getCurrentUser()
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    return@launch
                }

                val currentThread = _currentThread.value
                if (currentThread == null) {
                    _error.value = "No thread selected"
                    _isLoading.value = false
                    return@launch
                }

                if (_newMessageText.value.isBlank()) {
                    _error.value = "Message cannot be empty"
                    _isLoading.value = false
                    return@launch
                }

                // Generate a unique ID for the new message using UUID
                val newMessage = DiscussionMessage(
                    messageId = UUID.randomUUID().toString(),
                    userId = currentUser.userId,
                    text = _newMessageText.value
                )

                val result = discussionRepository.addMessage(currentThread.threadId, newMessage)
                result.fold(
                    onSuccess = {
                        _newMessageText.value = ""
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _error.value = error.message ?: "Failed to send message"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to send message"
                _isLoading.value = false
            }
        }
    }

    fun upvoteMessage(threadId: String, messageId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val currentMessage = _messages.value.find { it.messageId == messageId }
                if (currentMessage == null) {
                    _error.value = "Message not found"
                    _isLoading.value = false
                    return@launch
                }

                val updatedMessage = currentMessage.copy(
                    upvotes = currentMessage.upvotes + 1
                )

                val result = discussionRepository.updateMessage(threadId, updatedMessage)
                result.fold(
                    onSuccess = {
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _error.value = error.message ?: "Failed to upvote message"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to upvote message"
                _isLoading.value = false
            }
        }
    }

    fun resetDiscussionCreationStatus() {
        _discussionCreationSuccess.value = null
    }
    
    /**
     * Deletes a message from a thread.
     * 
     * This function checks if the current user has admin privileges before attempting
     * to delete the message. Only users with admin role can delete messages.
     * 
     * @param threadId The ID of the thread containing the message
     * @param messageId The ID of the message to delete
     */
    fun deleteMessage(threadId: String, messageId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val currentUser = authViewModel.getCurrentUser()
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    return@launch
                }
                
                // Check if user has admin privileges
                if (!authViewModel.hasPermission("admin")) {
                    _error.value = "You don't have permission to delete messages"
                    _isLoading.value = false
                    return@launch
                }
                
                val result = discussionRepository.deleteMessage(threadId, messageId)
                result.fold(
                    onSuccess = {
                        _isLoading.value = false
                        // Message deleted successfully - no need to do anything else
                        // as the observeThreadMessages will automatically update the UI
                    },
                    onFailure = { error ->
                        _error.value = error.message ?: "Failed to delete message"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete message"
                _isLoading.value = false
            }
        }
    }
}
