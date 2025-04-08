package edu.utap

import android.app.Application
import edu.utap.utils.ApplicationContextProvider
import edu.utap.utils.SecurePrefsProvider

/**
 * Custom [Application] class for the Flood Response app.
 *
 * Initializes application-wide components such as:
 * - [ApplicationContextProvider] for global context access
 * - [SecurePrefsProvider] for encrypted shared preferences
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
