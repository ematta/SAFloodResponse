package edu.utap.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import edu.utap.di.DiscussionModule
import edu.utap.repository.FloodReportRepository
import edu.utap.ui.viewmodel.AuthViewModel
import edu.utap.ui.viewmodel.DiscussionViewModel
import edu.utap.ui.viewmodel.FloodMapTestViewModel
import edu.utap.ui.viewmodel.FloodReportViewModel
import edu.utap.ui.viewmodel.UserViewModel
import edu.utap.repository.FirebaseUserRepository
import edu.utap.utils.FirebaseStorageUtil
import edu.utap.utils.FirebaseStorageUtilInterface
import edu.utap.utils.LocationUtils
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
class ViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {

    // Shared dependencies
    private val networkUtils: NetworkUtilsInterface = NetworkUtilsImpl()
    private val authRepository = AuthModule(
        FirebaseAuth.getInstance(),
        FirebaseFirestore.getInstance()
    ).provideAuthRepository()
    private val authViewModel: AuthViewModel = AuthViewModel(
        authRepository,
        networkUtils = networkUtils,
        context = context
    )
    private val locationUtils: LocationUtils = LocationUtils(context)
    private val storageUtil: FirebaseStorageUtilInterface = FirebaseStorageUtil()

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
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                authViewModel as T
            }
            modelClass.isAssignableFrom(DiscussionViewModel::class.java) -> {
                val discussionRepo = DiscussionModule.provideDiscussionRepository()
                DiscussionViewModel(discussionRepository = discussionRepo, authViewModel = authViewModel) as T
            }
            modelClass.isAssignableFrom(FloodMapTestViewModel::class.java) -> {
                val floodRepo = FloodReportRepository(FirebaseFirestore.getInstance())
                FloodMapTestViewModel(floodRepo) as T
            }
            modelClass.isAssignableFrom(FloodReportViewModel::class.java) -> {
                val floodRepo = FloodReportRepository(FirebaseFirestore.getInstance())
                FloodReportViewModel(
                    floodReportRepository = floodRepo,
                    authViewModel = authViewModel,
                    locationUtils = locationUtils,
                    storageUtil = storageUtil
                ) as T
            }
            modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                val app = context.applicationContext as Application
                UserViewModel(application = app, userRepository = FirebaseUserRepository(), storageUtil = storageUtil as FirebaseStorageUtil) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
