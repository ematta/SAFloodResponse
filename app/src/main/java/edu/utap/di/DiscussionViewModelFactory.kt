package edu.utap.di

import android.util.Log
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import edu.utap.ui.viewmodel.AuthViewModel
import edu.utap.ui.viewmodel.AuthViewModelInterface
import edu.utap.repository.DiscussionRepositoryInterface
import edu.utap.ui.viewmodel.DiscussionViewModel

/**
 * Factory for creating [DiscussionViewModel] instances with required dependencies.
 *
 * This factory initializes the discussion repository and authentication ViewModel,
 * handling exceptions and casting internally.
 *
 * Usage:
 * ```
 * val factory = DiscussionViewModelFactory.provideFactory(context)
 * val viewModel = ViewModelProvider(this, factory).get(DiscussionViewModel::class.java)
 * ```
 */
class DiscussionViewModelFactory private constructor() : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        throw UnsupportedOperationException("Use provideFactory() instead")
    }

    companion object {
        /**
         * Provides a [ViewModelProvider.Factory] that creates [DiscussionViewModel] instances.
         *
         * This factory initializes dependencies including the discussion repository
         * and authentication ViewModel.
         *
         * @param context Application context.
         * @return A [ViewModelProvider.Factory] for [DiscussionViewModel].
         */
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                /**
                 * Creates a [DiscussionViewModel] with injected dependencies.
                 *
                 * @param modelClass The ViewModel class.
                 * @return The created ViewModel instance.
                 * @throws IllegalArgumentException if the ViewModel class is unknown.
                 */
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val discussionRepository: DiscussionRepositoryInterface = try {
                        DiscussionModule.provideDiscussionRepository()
                    } catch (e: Exception) {
                        Log.e("DiscussionVMFactory", "Failed to provide DiscussionRepository", e)
                        throw e
                    }

                    val authViewModelAny: Any = try {
                        ViewModelFactory(context)
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
