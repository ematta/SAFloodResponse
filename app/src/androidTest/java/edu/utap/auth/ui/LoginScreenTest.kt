package edu.utap.auth.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.utap.MainActivity
import edu.utap.BuildConfig
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginScreen_displaysCorrectly() {
        // Verify login elements are displayed
        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
    }

    @Test
    fun loginScreen_errorOnEmptyFields() {
        // Click sign in without entering credentials
        composeTestRule.onNodeWithText("Sign In").performClick()
        
        // Verify error message is displayed
        composeTestRule.onNodeWithText("Please enter email and password").assertIsDisplayed()
    }

    @Test
    fun loginScreen_navigatesToMainScreenAfterSuccessfulLogin() {
        // Enter valid credentials
        composeTestRule.onNodeWithText("Email").performTextInput(BuildConfig.TEST_USERNAME)
        composeTestRule.onNodeWithText("Password").performTextInput(BuildConfig.TEST_PASSWORD)
        
        // Click sign in
        composeTestRule.onNodeWithText("Sign In").performClick()
        
        // Verify navigation to main screen (this will depend on your app's flow)
        // For example, if your main screen has a title:
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SA Flood Response").assertIsDisplayed()
    }
}
