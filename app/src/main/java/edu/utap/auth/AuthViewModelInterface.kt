package edu.utap.auth

import edu.utap.auth.utils.RoleUtils
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for AuthViewModel to allow for easier testing
 */
interface AuthViewModelInterface {
    val authState: StateFlow<AuthState>
    
    fun register(
        email: String, 
        password: String, 
        name: String, 
        role: String = RoleUtils.ROLE_REGULAR
    )
    fun login(email: String, password: String)
    fun logout()
    fun resetPassword(email: String)
}