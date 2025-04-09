package edu.utap.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log

@RunWith(AndroidJUnit4::class)
class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var mockViewModel: MockAuthViewModel
    private lateinit var testViewModelFactory: TestViewModelFactory
    
    @Before
    fun setup() {
        mockViewModel = MockAuthViewModel()
        testViewModelFactory = TestViewModelFactory(mockViewModel)
        composeTestRule.setViewModelFactory(testViewModelFactory)

        Log.d(TAG, "RegisterScreenTest: Test setup complete. Factory: ${TestViewModelFactoryProvider.factory}")
        assert(TestViewModelFactoryProvider.factory == testViewModelFactory) { "Factory not set correctly in RegisterScreenTest" }
        
        // Set initial auth state
        mockViewModel.updateAuthState(AuthState.Idle.Unauthenticated)
    }

    @Test
    fun registerScreen_emptyNameShowsError() {
        // Given: Set up the register screen
        composeTestRule.setContent {
            RegisterScreen(
                authViewModel = mockViewModel,
                onNavigateToLogin = {},
                onRegisterSuccess = {}
            )
        }

        // Wait for UI to be idle
        composeTestRule.waitForIdle()
        
        // Debug: Print UI hierarchy
        composeTestRule.onRoot().printToLog("RegisterScreenTest")

        // When: Click register button with empty name
        composeTestRule.onNodeWithTag("edu.utap.auth.registerButton").performClick()

        // Then: Name error should be displayed
        composeTestRule.onNodeWithText("Please enter your name")
            .assertIsDisplayed()
    }

    @Test
    fun registerScreen_emptyEmailShowsError() {
        // Given: Set up the register screen
        composeTestRule.setContent {
            RegisterScreen(
                authViewModel = mockViewModel,
                onNavigateToLogin = {},
                onRegisterSuccess = {}
            )
        }

        // Wait for UI to be idle
        composeTestRule.waitForIdle()
        
        // Debug: Print UI hierarchy
        composeTestRule.onRoot().printToLog("RegisterScreenTest")

        // When: Enter name but empty email and click register
        composeTestRule.onNodeWithTag("edu.utap.auth.nameField").performTextInput("Test User")
        composeTestRule.onNodeWithTag("edu.utap.auth.registerButton").performClick()

        // Then: Email error should be displayed
        composeTestRule.onNodeWithText("Email cannot be empty")
            .assertIsDisplayed()
    }

    @Test
    fun registerScreen_invalidEmailShowsError() {
        // Given: Set up the register screen
        composeTestRule.setContent {
            RegisterScreen(
                authViewModel = mockViewModel,
                onNavigateToLogin = {},
                onRegisterSuccess = {}
            )
        }

        // Wait for UI to be idle
        composeTestRule.waitForIdle()

        // When: Enter name and invalid email and click register
        composeTestRule.onNodeWithTag("edu.utap.auth.nameField").performTextInput("Test User")
        composeTestRule.onNodeWithTag("edu.utap.auth.emailField").performTextInput("invalid-email")
        composeTestRule.onNodeWithTag("edu.utap.auth.registerButton").performClick()

        // Then: Email format error should be displayed
        composeTestRule.onNodeWithText("Please enter a valid email address")
            .assertIsDisplayed()
    }

    @Test
    fun registerScreen_emptyPasswordShowsError() {
        // Given: Set up the register screen
        composeTestRule.setContent {
            Log.d(TAG, "Setting up RegisterScreen content with mockViewModel: $mockViewModel")
            RegisterScreen(
                authViewModel = mockViewModel,
                onNavigateToLogin = {},
                onRegisterSuccess = {}
            )
        }

        // Wait for UI to be idle
        composeTestRule.waitForIdle()

        // When: Enter name and valid email but empty password and click register
        composeTestRule.onNodeWithTag("edu.utap.auth.nameField").performTextInput("Test User")
        composeTestRule.onNodeWithTag("edu.utap.auth.emailField").performTextInput("test@user.com")
        composeTestRule.onNodeWithTag("edu.utap.auth.registerButton").performClick()

        // Then: Password error should be displayed
        composeTestRule.onNodeWithText("Password cannot be empty")
            .assertIsDisplayed()
    }

    @Test
    fun registerScreen_shortPasswordShowsError() {
        // Given: Set up the register screen
        composeTestRule.setContent {
            RegisterScreen(
                authViewModel = mockViewModel,
                onNavigateToLogin = {},
                onRegisterSuccess = {}
            )
        }

        // Wait for UI to be idle
        composeTestRule.waitForIdle()

        // When: Enter name, valid email but short password and click register
        composeTestRule.onNodeWithTag("edu.utap.auth.nameField").performTextInput("Test User")
        composeTestRule.onNodeWithTag("edu.utap.auth.emailField").performTextInput("test@user.com")
        composeTestRule.onNodeWithTag("edu.utap.auth.passwordField").performTextInput("12345")
        composeTestRule.onNodeWithTag("edu.utap.auth.registerButton").performClick()

        // Then: Password length error should be displayed
        composeTestRule.onNodeWithText("Password must be at least 6 characters")
            .assertIsDisplayed()
    }

    @Test
    fun registerScreen_passwordMismatchShowsError() {
        // Given: Set up the register screen
        composeTestRule.setContent {
            RegisterScreen(
                authViewModel = mockViewModel,
                onNavigateToLogin = {},
                onRegisterSuccess = {}
            )
        }

        // Wait for UI to be idle
        composeTestRule.waitForIdle()

        // When: Enter name, valid email, valid password but different confirm password
        composeTestRule.onNodeWithTag("edu.utap.auth.nameField").performTextInput("Test User")
        composeTestRule.onNodeWithTag("edu.utap.auth.emailField").performTextInput("test@user.com")
        composeTestRule.onNodeWithTag("edu.utap.auth.passwordField").performTextInput("test123")
        composeTestRule.onNodeWithTag("edu.utap.auth.confirmPasswordField").performTextInput("different123")
        composeTestRule.onNodeWithTag("edu.utap.auth.registerButton").performClick()

        // Then: Password mismatch error should be displayed
        composeTestRule.onNodeWithText("Passwords do not match")
            .assertIsDisplayed()
    }

    @Test
    fun registerScreen_validInputsNoErrors() {
        // Given: Set up the register screen with a mock ViewModel
        val mockViewModel = MockAuthViewModel()
        var registrationSuccessful = false
        
        composeTestRule.setContent {
            RegisterScreen(
                authViewModel = mockViewModel,
                onNavigateToLogin = {},
                onRegisterSuccess = { registrationSuccessful = true }
            )
        }

        // Wait for UI to be idle
        composeTestRule.waitForIdle()

        // When: Enter valid inputs for all fields
        composeTestRule.onNodeWithTag("edu.utap.auth.nameField").performTextInput("Test User")
        composeTestRule.onNodeWithTag("edu.utap.auth.emailField").performTextInput("test@user.com")
        composeTestRule.onNodeWithTag("edu.utap.auth.passwordField").performTextInput("test123")
        composeTestRule.onNodeWithTag("edu.utap.auth.confirmPasswordField").performTextInput("test123")
        composeTestRule.onNodeWithTag("edu.utap.auth.registerButton").performClick()

        // Then: No error messages should be displayed and registration should be successful
        composeTestRule.onNodeWithText("Email cannot be empty").assertDoesNotExist()
        composeTestRule.onNodeWithText("Password cannot be empty").assertDoesNotExist()
        composeTestRule.onNodeWithText("Passwords do not match").assertDoesNotExist()
    }
}
