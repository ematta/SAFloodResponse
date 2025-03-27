package edu.utap.auth.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.utap.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Before
    fun loginToApp() {
        // Login to the app before each test
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        composeTestRule.onNodeWithText("Sign In").performClick()
        
        // Wait for navigation to complete
        composeTestRule.waitForIdle()
    }

    @Test
    fun mainScreen_displaysUserInfo() {
        // Verify user information is displayed
        composeTestRule.onNodeWithText("Test User").assertIsDisplayed()
    }

    @Test
    fun mainScreen_navigationDrawerWorks() {
        // Open navigation drawer
        composeTestRule.onNodeWithContentDescription("Open navigation drawer").performClick()
        
        // Verify drawer items are displayed
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun mainScreen_logoutWorks() {
        // Open navigation drawer
        composeTestRule.onNodeWithContentDescription("Open navigation drawer").performClick()
        
        // Click on logout
        composeTestRule.onNodeWithText("Logout").performClick()
        
        // Verify we're back at the login screen
        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
    }
}
