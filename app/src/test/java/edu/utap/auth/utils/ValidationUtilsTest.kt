package edu.utap.auth.utils

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationUtilsTest {

    @Test
    fun `validateEmail returns valid for correct email format`() {
        // Given
        val email = "test@example.com"
        
        // When
        val result = ValidationUtils.validateEmail(email)
        
        // Then
        assertTrue(result.first)
        assertEquals(null, result.second)
    }
    
    @Test
    fun `validateEmail returns invalid for empty email`() {
        // Given
        val email = ""
        
        // When
        val result = ValidationUtils.validateEmail(email)
        
        // Then
        assertFalse(result.first)
        assertEquals("Email cannot be empty", result.second)
    }
    
    @Test
    fun `validateEmail returns invalid for malformed email`() {
        // Given
        val email = "invalid-email"
        
        // When
        val result = ValidationUtils.validateEmail(email)
        
        // Then
        assertFalse(result.first)
        assertEquals("Please enter a valid email address", result.second)
    }
    
    @Test
    fun `validatePassword returns valid for correct password length`() {
        // Given
        val password = "password123"
        
        // When
        val result = ValidationUtils.validatePassword(password)
        
        // Then
        assertTrue(result.first)
        assertEquals(null, result.second)
    }
    
    @Test
    fun `validatePassword returns invalid for empty password`() {
        // Given
        val password = ""
        
        // When
        val result = ValidationUtils.validatePassword(password)
        
        // Then
        assertFalse(result.first)
        assertEquals("Password cannot be empty", result.second)
    }
    
    @Test
    fun `validatePassword returns invalid for short password`() {
        // Given
        val password = "12345"
        
        // When
        val result = ValidationUtils.validatePassword(password)
        
        // Then
        assertFalse(result.first)
        assertEquals("Password must be at least 6 characters long", result.second)
    }
    
    @Test
    fun `validateLoginCredentials returns valid for correct credentials`() {
        // Given
        val email = "test@example.com"
        val password = "password123"
        
        // When
        val result = ValidationUtils.validateLoginCredentials(email, password)
        
        // Then
        assertTrue(result.first)
        assertEquals(null, result.second)
    }
    
    @Test
    fun `validateLoginCredentials returns invalid for invalid email`() {
        // Given
        val email = "invalid-email"
        val password = "password123"
        
        // When
        val result = ValidationUtils.validateLoginCredentials(email, password)
        
        // Then
        assertFalse(result.first)
        assertEquals("Please enter a valid email address", result.second)
    }
    
    @Test
    fun `validateLoginCredentials returns invalid for invalid password`() {
        // Given
        val email = "test@example.com"
        val password = "12345"
        
        // When
        val result = ValidationUtils.validateLoginCredentials(email, password)
        
        // Then
        assertFalse(result.first)
        assertEquals("Password must be at least 6 characters long", result.second)
    }
}