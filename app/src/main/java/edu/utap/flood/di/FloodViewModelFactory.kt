package edu.utap.flood.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import edu.utap.auth.di.AuthModule
import edu.utap.auth.model.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.flood.repository.FirestoreFloodReportRepository
import edu.utap.flood.repository.FloodReportRepository
import edu.utap.ui.viewmodel.FloodReportViewModel
import edu.utap.utils.LocationUtils

/**
 * Factory for creating FloodReportViewModel with dependencies.
 * This provides the necessary dependencies for the FloodReportViewModel constructor.
 */
class FloodViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(FloodReportViewModel::class.java) -> {
            // Get the AuthViewModel from the existing factory
            val authViewModel = AuthModule.provideAuthRepository().let { repo ->
                AuthViewModel(repo)
            }

            // Create the FirestoreFloodReportRepository
            val firestore = FirebaseFirestore.getInstance()
            val floodReportRepository = FirestoreFloodReportRepository(firestore)
            println("FirestoreFloodReportRepository initialized with Firestore instance")
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
