package edu.utap.flood.di

import android.util.Log
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import edu.utap.auth.model.AuthViewModel
import edu.utap.auth.model.AuthViewModelInterface
import edu.utap.flood.repository.DiscussionRepositoryInterface
import edu.utap.ui.viewmodel.DiscussionViewModel

class DiscussionViewModelFactory private constructor() : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        throw UnsupportedOperationException("Use provideFactory() instead")
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val discussionRepository: DiscussionRepositoryInterface = try {
                        DiscussionModule.provideDiscussionRepository()
                    } catch (e: Exception) {
                        Log.e("DiscussionVMFactory", "Failed to provide DiscussionRepository", e)
                        throw e
                    }

                    val authViewModelAny: Any = try {
                        edu.utap.auth.di.ViewModelFactory(context)
                            .create(AuthViewModel::class.java)
                    } catch (e: Exception) {
                        Log.e("DiscussionVMFactory", "Failed to create AuthViewModel", e)
                        throw e
                    }

                    val authViewModel: AuthViewModelInterface = try {
                        authViewModelAny as AuthViewModelInterface
                    } catch (e: Exception) {
                        Log.e("DiscussionVMFactory", "Failed to cast AuthViewModel to AuthViewModelInterface", e)
                        throw e
                    }

                    return try {
                        DiscussionViewModel(
                            discussionRepository = discussionRepository,
                            authViewModel = authViewModel
                        ) as T
                    } catch (e: Exception) {
                        Log.e("DiscussionVMFactory", "Failed to instantiate DiscussionViewModel", e)
                        throw e
                    }
                }
            }
        }
    }
}
