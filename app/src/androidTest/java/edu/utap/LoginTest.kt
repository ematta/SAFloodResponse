package edu.utap

import androidx.compose.ui.test.*
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.utap.ui.screens.LoginScreen
import edu.utap.ui.viewmodel.AuthViewModelInterface
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val emailField = hasTestTag("emailField")
    private val passwordField = hasTestTag("passwordField")
    private val loginButton = hasTestTag("loginButton")
    private val forgotPasswordButton = hasTestTag("forgotPasswordButton")
    private val registerButton = hasTestTag("registerButton")
    private val loadingSpinner = hasTestTag("loadingSpinner")
    private val errorMessage = hasTestTag("errorMessage")

    private class FakeAuthViewModel : AuthViewModelInterface {
        override val authState = kotlinx.coroutines.flow.MutableStateFlow<edu.utap.auth.AuthState>(edu.utap.auth.AuthState.Idle.Unauthenticated)

        override fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
            if (email == "user@example.com" && password == "validPassword123") {
                authState.value = edu.utap.auth.AuthState.Idle.Authenticated
                callback(true, null)
            } else {
                // Instead of sealed class error, set Unauthenticated and callback with error
                authState.value = edu.utap.auth.AuthState.Idle.Unauthenticated
                callback(false, "Invalid credentials")
            }
        }

        override fun logout(): edu.utap.auth.AuthState.Idle.Unauthenticated {
            authState.value = edu.utap.auth.AuthState.Idle.Unauthenticated
            return edu.utap.auth.AuthState.Idle.Unauthenticated
        }

        override fun register(
            email: String,
            password: String,
            name: String,
            role: String,
            callback: (Boolean, String?) -> Unit
        ) {
            callback(true, null)
        }

        override fun resetPassword(email: String, callback: (Boolean, String?) -> Unit) {
            callback(true, null)
        }

        override fun updateAuthState(sent: edu.utap.auth.AuthState) {}

        override fun restoreAuthState() {}

        override fun getCurrentUser(): edu.utap.models.FirestoreUser? = null
    }

    private fun setLoginScreen() {
        composeTestRule.setContent {
            LoginScreen(
                authViewModel = FakeAuthViewModel(),
                onNavigateToRegister = {},
                onNavigateToForgotPassword = {},
                onLoginSuccess = {}
            )
        }
    }

    @Test
    fun initialState_loginButtonDisabledOrEnabledBasedOnValidation() {
        setLoginScreen()
        composeTestRule.onNode(loginButton).assertExists()
        composeTestRule.onNode(passwordField).assertExists()
    }

    @Test
    fun invalidInput_showsValidationErrors() {
        setLoginScreen()
        composeTestRule.onNode(emailField).performTextInput("invalid-email")
        composeTestRule.onNode(passwordField).performTextInput("123")
        // reset focus
        composeTestRule.onNode(loginButton).requestFocus()
        composeTestRule.onNode(loginButton).performClick()
        // wait a second
        Thread.sleep(5000)
        composeTestRule.onNodeWithText("Invalid email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password too short").assertIsDisplayed()
    }

    @Test
    fun forgotPasswordButton_triggersNavigation() {
        setLoginScreen()
        composeTestRule.onNode(forgotPasswordButton).performClick()
        // Verify navigation effect, e.g., Forgot Password screen visible
        composeTestRule.onNodeWithText("Reset Password").assertIsDisplayed()
    }

    @Test
    fun registerButton_triggersNavigation() {
        setLoginScreen()
        composeTestRule.onNode(registerButton).performClick()
        // Verify navigation effect, e.g., Register screen visible
        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
    }

    @Test
    fun loadingState_showsSpinnerAndDisablesLoginButton() {
        setLoginScreen()
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
        setLoginScreen()
        // Simulate successful login
        composeTestRule.onNode(emailField).performTextInput("user@example.com")
        composeTestRule.onNode(passwordField).performTextInput("validPassword123")
        composeTestRule.onNode(loginButton).performClick()

        // Verify navigation, e.g., Dashboard or Home screen visible
        composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
    }

    @Test
    fun failedLogin_showsErrorMessage() {
        setLoginScreen()
        // Simulate failed login
        composeTestRule.onNode(emailField).performTextInput("user@example.com")
        composeTestRule.onNode(passwordField).performTextInput("wrongPassword")
        composeTestRule.onNode(loginButton).performClick()

        // Verify error message displayed
        composeTestRule.onNode(errorMessage).assertIsDisplayed()
        composeTestRule.onNode(errorMessage).assertTextContains("Invalid credentials")
    }
}
