package edu.utap.auth

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Mock implementation of AuthViewModelInterface for testing
 */
class MockAuthViewModel(
    private val initialState: AuthState = AuthState.Idle,
    private val registerBehavior: (String, String, String) -> AuthState = { _, _, _ -> AuthState.Unauthenticated }
) : ViewModel(), AuthViewModelInterface {
    
    private val _authState = MutableStateFlow<AuthState>(initialState)
    override val authState: StateFlow<AuthState> = _authState
    
    override fun register(email: String, password: String, name: String) {
        _authState.value = AuthState.Loading
        _authState.value = registerBehavior(email, password, name)
    }
    
    override fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        _authState.value = AuthState.Unauthenticated
    }
    
    override fun logout() {
        _authState.value = AuthState.Unauthenticated
    }
}