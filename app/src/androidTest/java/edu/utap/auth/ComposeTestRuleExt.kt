package edu.utap.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Extension function for ComposeContentTestRule to set a custom ViewModelFactory
 * for testing composables that use ViewModels.
 */
fun ComposeContentTestRule.setViewModelFactory(factory: ViewModelProvider.Factory) {
    // Store the factory for later use in setContent calls
    TestViewModelFactoryProvider.factory = factory
    println("setViewModelFactory called with factory: $factory")
}

/**
 * Object to hold the ViewModelFactory for tests
 */
object TestViewModelFactoryProvider {
    var factory: ViewModelProvider.Factory? = null
}

/**
 * Extension function to wrap content with the test ViewModelFactory
 */
fun ComposeContentTestRule.setContentWithTestViewModelFactory(content: @Composable () -> Unit) {
    this.setContent {
        // Simply call the content function directly
        // The TestViewModelFactoryProvider.factory will be used by the viewModel() function
        // through the ViewModelFactoryHolder pattern in the test code
        content()
    }
}

/**
 * Extension function to create a ViewModel with the test factory
 */
@Composable
inline fun <reified VM : ViewModel> createTestViewModel(): VM {
    val factory = TestViewModelFactoryProvider.factory
    requireNotNull(factory) { "TestViewModelFactoryProvider.factory is null" }
    println("createTestViewModel: Factory is being used to create ViewModel of type: ${VM::class.java}")
    return viewModel(factory = factory)
}
