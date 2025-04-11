package edu.utap.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.repository.FloodReportRepository
import edu.utap.ui.viewmodel.AuthViewModel
import edu.utap.ui.viewmodel.FloodReportViewModel
import edu.utap.utils.LocationUtils
import edu.utap.utils.NetworkUtils

/**
 * Factory for creating [FloodReportViewModel] instances with required dependencies.
 *
 * This factory initializes the flood report repository, weather repository,
 * authentication ViewModel, and location utilities.
 *
 * @param context Application context.
 * @param floodReportRepository Repository for flood report data.
 * @param weatherRepository Repository for weather data.
 */
class FloodViewModelFactory(
    private val context: Context,
    private val floodReportRepository: FloodReportRepository,
    private val networkUtils: NetworkUtils
) : ViewModelProvider.Factory {

    /**
     * Creates a [FloodReportViewModel] with injected dependencies.
     *
     * @param modelClass The ViewModel class.
     * @return The created ViewModel instance.
     * @throws IllegalArgumentException if the ViewModel class is unknown.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(FloodReportViewModel::class.java) -> {
            // Get the AuthViewModel from the existing factory
            val authViewModel = AuthModule(
                firebaseAuth = FirebaseAuth.getInstance(),
                firestore = FirebaseFirestore.getInstance()
            ).provideAuthRepository().let { repo ->
                AuthViewModel(
                    repo,
                    networkUtils = networkUtils,
                    context = context
                )
            }

            // Create the LocationUtils
            val locationUtils = LocationUtils(context)

            // Create the FloodReportViewModel with all required dependencies
            FloodReportViewModel(
                floodReportRepository = floodReportRepository,
                authViewModel = authViewModel,
                locationUtils = locationUtils
            ) as T
        }
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
