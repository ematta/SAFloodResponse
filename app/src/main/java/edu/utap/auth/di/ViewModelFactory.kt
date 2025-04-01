package edu.utap.auth.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import edu.utap.auth.model.AuthViewModel

/**
 * Factory for creating ViewModels with dependencies.
 * This is a simplified version of what DI frameworks like Hilt or Dagger would provide.
 */
class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
            val authRepository = AuthModule.provideAuthRepository()
            AuthViewModel(
                authRepository
            ) as T
        }
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
