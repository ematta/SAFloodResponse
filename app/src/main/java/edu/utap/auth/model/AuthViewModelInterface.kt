package edu.utap.auth.model

import edu.utap.auth.AuthState
import edu.utap.db.UserEntity
import edu.utap.utils.RoleUtils
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
        role: String = RoleUtils.ROLE_REGULAR,
        function: (boolean: Boolean, message: String?) -> Unit = { _, _ -> }
    )
    fun login(
        email: String,
        password: String,
        function: (boolean: Boolean, message: String?) -> Unit
    )
    fun logout()
    fun resetPassword(email: String, callback: (Boolean, String?) -> Unit)
    fun getCurrentUser(): UserEntity?
    fun restoreAuthState()
    fun updateAuthState(sent: AuthState)
}
