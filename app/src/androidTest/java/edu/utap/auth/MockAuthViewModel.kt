package edu.utap.auth

import androidx.lifecycle.ViewModel
import edu.utap.models.AuthViewModelInterface
import edu.utap.models.FirestoreUser
import edu.utap.utils.RoleUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Mock implementation of AuthViewModelInterface for testing purposes
class MockAuthViewModel : ViewModel(), AuthViewModelInterface {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle.Unauthenticated)
    override val authState: StateFlow<AuthState> get() = _authState

    override fun register(
        email: String,
        password: String,
        name: String,
        role: String,
        function: (boolean: Boolean, message: String?) -> Unit
    ) {
        function(true, "Mock registration successful")
    }

    override fun login(
        email: String,
        password: String,
        function: (boolean: Boolean, message: String?) -> Unit
    ) {
        function(true, "Mock login successful")
    }

    override fun logout(): AuthState.Idle.Unauthenticated {
        _authState.value = AuthState.Idle.Unauthenticated
        return AuthState.Idle.Unauthenticated
    }

    override fun resetPassword(email: String, callback: (Boolean, String?) -> Unit) {
        callback(true, "Mock password reset successful")
    }

    override fun getCurrentUser(): FirestoreUser? {
        return FirestoreUser("mockUserId", "mockName", "mockEmail", RoleUtils.ROLE_REGULAR)
    }

    override fun restoreAuthState() {
        _authState.value = AuthState.Idle.Unauthenticated
    }

    override fun updateAuthState(sent: AuthState) {
        _authState.value = sent
    }
}
