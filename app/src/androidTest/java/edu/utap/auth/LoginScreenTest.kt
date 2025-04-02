package edu.utap.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var mockViewModel: MockAuthViewModel
    private lateinit var testViewModelFactory: TestViewModelFactory
    
    @Before
    fun setup() {
        mockViewModel = MockAuthViewModel()
        testViewModelFactory = TestViewModelFactory(mockViewModel)
        composeTestRule.setViewModelFactory(testViewModelFactory)
        assert(TestViewModelFactoryProvider.factory == testViewModelFactory) { "Factory not set correctly in LoginScreenTest" }
        println("LoginScreenTest: Test setup complete. Factory: ${TestViewModelFactoryProvider.factory}")
    }

    @Test
    fun loginScreen_emptyEmailShowsError() {
        // Given: Set up the login screen
        composeTestRule.setContent {
            LoginScreen(
                onNavigateToRegister = {},
                onLoginSuccess = {},
                onNavigateToForgotPassword = {}
            )
        }

        // When: Click login button with empty email
        composeTestRule.onNodeWithTag("loginButton").performClick()

        // Then: Email error should be displayed
        composeTestRule.onNodeWithText("Email cannot be empty").assertIsDisplayed()
    }

    @Test
    fun loginScreen_invalidEmailShowsError() {
        // Given: Set up the login screen
        composeTestRule.setContent {
            LoginScreen(
                onNavigateToRegister = {},
                onLoginSuccess = {},
                onNavigateToForgotPassword = {}
            )
        }

        // When: Enter invalid email and click login
        composeTestRule.onNodeWithText("Email").performTextInput("invalid-email")
        composeTestRule.onNodeWithTag("loginButton").performClick()

        // Then: Email format error should be displayed
        composeTestRule.onNodeWithText("Please enter a valid email address").assertIsDisplayed()
    }

    @Test
    fun loginScreen_emptyPasswordShowsError() {
        // Given: Set up the login screen
        composeTestRule.setContent {
            LoginScreen(
                onNavigateToRegister = {},
                onLoginSuccess = {},
                onNavigateToForgotPassword = {}
            )
        }

        // When: Enter valid email but empty password and click login
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("loginButton").performClick()

        // Then: Password error should be displayed
        composeTestRule.onNodeWithText("Password cannot be empty").assertIsDisplayed()
    }

    @Test
    fun loginScreen_shortPasswordShowsError() {
        // Given: Set up the login screen
        composeTestRule.setContent {
            LoginScreen(
                onNavigateToRegister = {},
                onLoginSuccess = {},
                onNavigateToForgotPassword = {}
            )
        }

        // When: Enter valid email but short password and click login
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("12345")
        composeTestRule.onNodeWithTag("loginButton").performClick()

        // Then: Password length error should be displayed
        composeTestRule.onNodeWithText("Password must be at least 6 characters long").assertIsDisplayed()
    }

    @Test
    fun loginScreen_validInputsNoErrors() {
        // Given: Set up the login screen with a mock ViewModel
        val mockViewModel = MockAuthViewModel()
        var loginSuccessful = false
        
        composeTestRule.setContent {
            LoginScreen(
                authViewModel = createTestViewModel(),
                onNavigateToRegister = {},
                onLoginSuccess = { loginSuccessful = true },
                onNavigateToForgotPassword = {}
            )
        }

        // When: Enter valid inputs for all fields
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        composeTestRule.onNodeWithTag("loginButton").performClick()

        // Then: No error messages should be displayed
        composeTestRule.onNodeWithText("Email cannot be empty").assertDoesNotExist()
        composeTestRule.onNodeWithText("Password cannot be empty").assertDoesNotExist()
    }
}
