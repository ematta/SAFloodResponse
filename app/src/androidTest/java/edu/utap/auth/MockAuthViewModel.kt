package edu.utap.auth

import androidx.lifecycle.ViewModel
import edu.utap.auth.model.AuthViewModelInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Mock implementation of AuthViewModelInterface for testing
 */
class MockAuthViewModel(
    private val initialState: AuthState = AuthState.Idle.Initial,
    private val registerBehavior: (String, String, String) -> AuthState = { _, _, _ -> AuthState.Idle.Unauthenticated }
) : ViewModel(), AuthViewModelInterface {
    
    private val _authState = MutableStateFlow<AuthState>(initialState)
    override val authState: StateFlow<AuthState> = _authState
    override fun register(
        email: String,
        password: String,
        name: String,
        role: String
    ) {
        _authState.value = AuthState.Loading.Registration
        _authState.value = registerBehavior(email, password, name)
    }

    override fun login(email: String, password: String, function: (ERROR) -> Unit) {
        _authState.value = AuthState.Loading.Login
        _authState.value = AuthState.Idle.Unauthenticated
    }
    
    override fun logout() {
        _authState.value = AuthState.Idle.Unauthenticated
    }
    
    override fun resetPassword(email: String) {
        _authState.value = AuthState.Loading.PasswordReset
        _authState.value = AuthState.Idle.PasswordResetSent
    }
}
