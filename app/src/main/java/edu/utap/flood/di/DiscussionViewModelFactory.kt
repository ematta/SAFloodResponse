package edu.utap.flood.di

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
                    val discussionRepository = DiscussionModule.provideDiscussionRepository()
                    val authViewModel = edu.utap.auth.di.ViewModelFactory(context)
                        .create(AuthViewModel::class.java) as AuthViewModelInterface
                    return DiscussionViewModel(
                        discussionRepository = discussionRepository,
                        authViewModel = authViewModel
                    ) as T
                }
            }
        }
    }
}
