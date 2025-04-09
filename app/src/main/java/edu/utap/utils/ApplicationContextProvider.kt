package edu.utap.utils

import android.content.Context

/**
 * Interface for providing application context.
 */
interface ApplicationContextProviderInterface {
    fun getApplicationContext(): Context
}

/**
 * Default implementation that simply returns the provided context.
 */
class DefaultApplicationContextProvider(
    private val context: Context
) : ApplicationContextProviderInterface {
    override fun getApplicationContext(): Context = context
}
