package edu.utap

import android.app.Application
import edu.utap.utils.ApplicationContextProvider
import edu.utap.utils.SecurePrefsProvider

/**
 * Application class for the Flood Response app
 * Initializes application-wide components
 */
class FloodResponseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the ApplicationContextProvider with the application context
        ApplicationContextProvider.init(this)

        // Initialize EncryptedSharedPreferences
        SecurePrefsProvider.init(this)
    }
}
