package edu.utap.user

import android.content.Context
import edu.utap.auth.utils.ApplicationContextProvider
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject

/**
 * Helper class to mock ApplicationContextProvider for tests
 */
class TestApplicationContextProvider {
    companion object {
        /**
         * Mock the ApplicationContextProvider to return the provided context
         * @param context The context to return from getApplicationContext()
         */
        fun mockApplicationContext(context: Context) {
            mockkObject(ApplicationContextProvider)
            every { ApplicationContextProvider.getApplicationContext() } returns context
        }
        
        /**
         * Reset the ApplicationContextProvider mock
         */
        fun resetMock() {
            unmockkObject(ApplicationContextProvider)
        }
    }
}