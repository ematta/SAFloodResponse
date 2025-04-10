package edu.utap.utils

import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import io.mockk.mockk

class DefaultApplicationContextProviderTest {

    @Test
    fun `Context object is not null`() {
        val mockContext = mockk<Context>()
        val provider = DefaultApplicationContextProvider(mockContext)
        val context = provider.getApplicationContext()
        assertNotNull("Context should not be null", context)
    }

    @Test
    fun `Correct context object returned`() {
        val mockContext = mockk<Context>()
        val provider = DefaultApplicationContextProvider(mockContext)
        val context = provider.getApplicationContext()
        assertSame("Returned context should be the same as the one passed in constructor", mockContext, context)
    }

    @Test
    fun `Context type is correct`() {
        val mockContext = mockk<Context>()
        val provider = DefaultApplicationContextProvider(mockContext)
        val context = provider.getApplicationContext()
        assertTrue("Returned object should be instance of Context", context is Context)
    }

    @Test(expected = NullPointerException::class)
    fun `Null Context Handling`() {
        @Suppress("UNCHECKED_CAST")
        val nullContext = null as Context
        DefaultApplicationContextProvider(nullContext)
    }

    @Test
    fun `Context Immutability check`() {
        val mockContext = mockk<Context>()
        val provider = DefaultApplicationContextProvider(mockContext)
        val context1 = provider.getApplicationContext()
        val context2 = provider.getApplicationContext()
        assertSame("Context should be immutable and same reference returned", context1, context2)
    }

    @Test
    fun `Multiple Calls Consistency Check`() {
        val mockContext = mockk<Context>()
        val provider = DefaultApplicationContextProvider(mockContext)
        val firstCall = provider.getApplicationContext()
        val secondCall = provider.getApplicationContext()
        val thirdCall = provider.getApplicationContext()
        assertSame("All calls should return the same context instance", firstCall, secondCall)
        assertSame("All calls should return the same context instance", firstCall, thirdCall)
    }

    @Test
    fun `Context State After Initialization`() {
        val mockContext = mockk<Context>()
        val provider = DefaultApplicationContextProvider(mockContext)
        val context = provider.getApplicationContext()
        assertSame("Context state should remain consistent after initialization", mockContext, context)
    }
}
