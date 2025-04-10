package edu.utap

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.utap.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val emailField = hasTestTag("emailField")
    private val passwordField = hasTestTag("passwordField")
    private val loginButton = hasTestTag("loginButton")
    private val forgotPasswordButton = hasTestTag("forgotPasswordButton")
    private val registerButton = hasTestTag("registerButton")
    private val loadingSpinner = hasTestTag("loadingSpinner")
    private val errorMessage = hasTestTag("errorMessage")

    @Test
    fun initialState_loginButtonDisabledOrEnabledBasedOnValidation() {
        composeTestRule.onNode(loginButton).assertExists()
        // Assuming initially empty fields, so button should be disabled
        composeTestRule.onNode(loginButton).assertIsNotEnabled()
    }

    @Test
    fun invalidInput_showsValidationErrors() {
        composeTestRule.onNode(emailField).performTextInput("invalid-email")
        composeTestRule.onNode(passwordField).performTextInput("123")
        composeTestRule.onNode(loginButton).performClick()

        // Check for validation error messages (assuming testTags or text)
        composeTestRule.onNodeWithText("Invalid email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password too short").assertIsDisplayed()
    }

    @Test
    fun forgotPasswordButton_triggersNavigation() {
        composeTestRule.onNode(forgotPasswordButton).performClick()
        // Verify navigation effect, e.g., Forgot Password screen visible
        composeTestRule.onNodeWithText("Reset Password").assertIsDisplayed()
    }

    @Test
    fun registerButton_triggersNavigation() {
        composeTestRule.onNode(registerButton).performClick()
        // Verify navigation effect, e.g., Register screen visible
        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
    }

    @Test
    fun loadingState_showsSpinnerAndDisablesLoginButton() {
        // Simulate loading state by clicking login with valid input
        composeTestRule.onNode(emailField).performTextInput("user@example.com")
        composeTestRule.onNode(passwordField).performTextInput("validPassword123")
        composeTestRule.onNode(loginButton).performClick()

        // Check spinner appears and button disabled during loading
        composeTestRule.onNode(loadingSpinner).assertIsDisplayed()
        composeTestRule.onNode(loginButton).assertIsNotEnabled()
    }

    @Test
    fun successfulLogin_navigatesToNextScreen() {
        // Simulate successful login
        composeTestRule.onNode(emailField).performTextInput("user@example.com")
        composeTestRule.onNode(passwordField).performTextInput("validPassword123")
        composeTestRule.onNode(loginButton).performClick()

        // Verify navigation, e.g., Dashboard or Home screen visible
        composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
    }

    @Test
    fun failedLogin_showsErrorMessage() {
        // Simulate failed login
        composeTestRule.onNode(emailField).performTextInput("user@example.com")
        composeTestRule.onNode(passwordField).performTextInput("wrongPassword")
        composeTestRule.onNode(loginButton).performClick()

        // Verify error message displayed
        composeTestRule.onNode(errorMessage).assertIsDisplayed()
        composeTestRule.onNode(errorMessage).assertTextContains("Invalid credentials")
    }
}
