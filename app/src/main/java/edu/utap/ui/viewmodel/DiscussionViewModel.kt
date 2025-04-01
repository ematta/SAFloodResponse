package edu.utap.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.utap.auth.model.AuthViewModelInterface
import edu.utap.flood.model.DiscussionMessage
import edu.utap.flood.model.DiscussionThread
import edu.utap.flood.repository.DiscussionRepositoryInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Initialize by loading all threads
    init {
        fetchAllThreads()
    }

    fun updateNewMessageText(text: String) {
        _newMessageText.value = text
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
                
                val newMessage = DiscussionMessage(
                    messageId = System.currentTimeMillis().toString(),
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
}
