package edu.utap.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import edu.utap.models.AuthViewModel

/**
 * A test-specific ViewModelFactory that provides mock implementations
 * of ViewModels for testing purposes.
 */
class TestViewModelFactory(
    private val mockAuthViewModel: MockAuthViewModel
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                mockAuthViewModel as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
