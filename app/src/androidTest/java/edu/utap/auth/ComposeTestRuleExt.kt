package edu.utap.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log

const val TAG = "AuthModule"

/**
 * Extension function for ComposeContentTestRule to set a custom ViewModelFactory
 * for testing composables that use ViewModels.
 */
fun ComposeContentTestRule.setViewModelFactory(factory: ViewModelProvider.Factory) {
    // Store the factory for later use in setContent calls
    TestViewModelFactoryProvider.factory = factory
    Log.d(TAG, "setViewModelFactory called with factory: $factory")
}

/**
 * Object to hold the ViewModelFactory for tests
 */
object TestViewModelFactoryProvider {
    var factory: ViewModelProvider.Factory? = null
}

/**
 * Custom matcher to check if a text field has an error message
 */
fun hasTextInputError(expectedError: String): SemanticsMatcher {
    return SemanticsMatcher("Text field has error: $expectedError") { node ->
        val errorMessage = node.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()
        errorMessage?.contains(expectedError) ?: false
    }
}

/**
 * Custom matcher to check if a text field has no error
 */
fun hasNoTextInputError(): SemanticsMatcher {
    return SemanticsMatcher("Text field has no error") { node ->
        node.config.getOrNull(SemanticsProperties.Error) == null
    }
}

/**
 * Extension function to wrap content with the test ViewModelFactory
 */
fun AndroidComposeTestRule<*, *>.setContentWithTestViewModelFactory(content: @Composable () -> Unit) {
    this.setContent {
        val factory = TestViewModelFactoryProvider.factory
        requireNotNull(factory) { "TestViewModelFactoryProvider.factory must be set before calling setContentWithTestViewModelFactory" }
        
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides this.activity,
        ) {
            content()
        }
    }
}

/**
 * Extension function to create a ViewModel with the test factory
 */
@Composable
inline fun <reified VM : ViewModel> createTestViewModel(): VM {
    val factory = TestViewModelFactoryProvider.factory
    requireNotNull(factory) { "TestViewModelFactoryProvider.factory is null" }
    Log.d(TAG, "createTestViewModel: Factory is being used to create ViewModel of type: ${VM::class.java}")
    return viewModel(factory = factory)
}
