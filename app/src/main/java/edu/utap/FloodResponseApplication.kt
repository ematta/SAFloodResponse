package edu.utap

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * [FloodResponseApplication] is a custom Application class for the Flood Response app.
 *
 * It extends the base [Application] class and is responsible for:
 * 1. Initializing Firebase on application startup.
 * 2. Configuring Firebase App Check with either Debug or Play Integrity providers based on the build type.
 *
 * This ensures that the Firebase services are ready to use when the application starts and that App Check is properly configured to help prevent abuse.
 */
class FloodResponseApplication : Application() {
    /**
     * Called when the activity is first created.
     *
     * This method performs the following initialization tasks:
     * 1. **Calls the superclass's onCreate()**: Ensures the parent class's setup is performed.
     * 2. **Initializes Firebase**: Sets up Firebase for the application using `FirebaseApp.initializeApp(this)`.
     * 3. **Installs Firebase App Check**: Configures Firebase App Check to help protect your backend resources.
     *    - **Debug Mode**: If the application is in debug mode (as determined by `BuildConfig.DEBUG`),
     *      it installs `DebugAppCheckProviderFactory` for testing and development. This provider
     *      allows for bypassing App Check during debugging.
     *    - **Release Mode**: If the application is in release mode, it installs
     *      `PlayIntegrityAppCheckProviderFactory`, which uses Google Play Integrity to verify
     *      the legitimacy of the app's installation and prevent unauthorized access.
     *
     * @see AppCompatActivity.onCreate
     */
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        // Install Firebase App Check provider
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
