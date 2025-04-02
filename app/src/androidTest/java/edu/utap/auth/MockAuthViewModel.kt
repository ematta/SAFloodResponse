package edu.utap.auth

import androidx.lifecycle.ViewModel
import edu.utap.auth.model.AuthViewModelInterface
import edu.utap.auth.model.FirestoreUser
import com.google.firebase.auth.FirebaseUser
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
        role: String,
        function: (Boolean, String?) -> Unit
    ) {
        _authState.value = AuthState.Loading.Registration
        _authState.value = registerBehavior(email, password, name)
        function(true, null)
    }

    override fun login(
        email: String,
        password: String,
        function: (Boolean, String?) -> Unit
    ) {
        _authState.value = AuthState.Loading.Login
        _authState.value = AuthState.Idle.Unauthenticated
        function(true, null)
    }

    override fun logout() {
        _authState.value = AuthState.Idle.Unauthenticated
    }

    override fun resetPassword(
        email: String,
        callback: (Boolean, String?) -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override fun restoreAuthState() {
        _authState.value = initialState
    }

    override fun updateAuthState(sent: AuthState) {
        _authState.value = sent
    }

    override fun getCurrentUser(): FirestoreUser? {
        return null // Mock implementation returns null
    }
}
