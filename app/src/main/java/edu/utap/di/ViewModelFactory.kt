package edu.utap.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.ui.viewmodel.AuthViewModel
import edu.utap.utils.NetworkUtilsInterface
import edu.utap.utils.NetworkUtilsImpl

/**
 * Factory for creating ViewModels with dependencies.
 * This is a simplified version of what DI frameworks like Hilt or Dagger would provide.
 */
/**
 * Factory for creating ViewModels with required dependencies.
 *
 * This avoids tight coupling and allows injecting repositories or other dependencies.
 * In a production app, this would typically be replaced by a DI framework like Hilt or Dagger.
 *
 * @param context Application context, if needed for ViewModel dependencies.
 */
class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    /**
     * Creates a ViewModel instance of the given class, injecting dependencies.
     *
     * Currently supports:
     * - [AuthViewModel]
     *
     * @param modelClass The class of the ViewModel to create.
     * @return The created ViewModel instance.
     * @throws IllegalArgumentException if the ViewModel class is unknown.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
            val networkUtils: NetworkUtilsInterface = NetworkUtilsImpl()
            val authRepository = AuthModule(
                FirebaseAuth.getInstance(),
                FirebaseFirestore.getInstance()
            ).provideAuthRepository()
            AuthViewModel(
                authRepository,
                networkUtils = networkUtils,
                context = context
            ) as T
        }
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
