package edu.utap.flood.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import edu.utap.auth.di.AuthModule
import edu.utap.auth.model.AuthViewModel
import edu.utap.flood.repository.FloodReportRepository
import edu.utap.ui.viewmodel.FloodReportViewModel
import edu.utap.utils.LocationUtils
import edu.utap.weather.repository.WeatherRepositoryImpl

/**
 * Factory for creating FloodReportViewModel with dependencies.
 * This provides the necessary dependencies for the FloodReportViewModel constructor.
 */
class FloodViewModelFactory(
    private val context: Context,
    private val floodReportRepository: FloodReportRepository,
    private val weatherRepository: WeatherRepositoryImpl
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(FloodReportViewModel::class.java) -> {
            // Get the AuthViewModel from the existing factory
            val authViewModel = AuthModule.provideAuthRepository().let { repo ->
                AuthViewModel(repo)
            }

            // Create the LocationUtils
            val locationUtils = LocationUtils(context)

            // Create the FloodReportViewModel with all required dependencies
            FloodReportViewModel(
                floodReportRepository = floodReportRepository,
                authViewModel = authViewModel,
                locationUtils = locationUtils,
                weatherRepository = weatherRepository
            ) as T
        }
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
