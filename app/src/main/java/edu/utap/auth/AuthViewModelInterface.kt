package edu.utap.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for AuthViewModel to allow for easier testing
 */
interface AuthViewModelInterface {
    val authState: StateFlow<AuthState>
    
    fun register(email: String, password: String, name: String)
    fun login(email: String, password: String)
    fun logout()
}