package edu.utap.auth.utils

import android.app.Application
import android.content.Context

/**
 * A singleton class that provides application context to non-UI components.
 * This helps avoid memory leaks by ensuring we don't hold references to Activity contexts.
 */
object ApplicationContextProvider {
    private lateinit var applicationContext: Context

    /**
     * Initialize with the application context
     * This should be called in the Application class's onCreate method
     */
    fun init(application: Application) {
        applicationContext = application.applicationContext
    }

    /**
     * Get the application context
     */
    fun getApplicationContext(): Context {
        if (!::applicationContext.isInitialized) {
            throw IllegalStateException(
                "ApplicationContextProvider not initialized. Call init() first."
            )
        }
        return applicationContext
    }
}
