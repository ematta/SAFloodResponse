package edu.utap.auth

import edu.utap.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages authentication flow, state restoration, and exposes authentication state.
 * Delegates actual auth operations to AuthViewModel.
 */
class AuthFlowManager(
    val authViewModel: AuthViewModel
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        // Restore auth state
        authViewModel.restoreAuthState()

        // Check cached user and expiration
        val cachedUser = authViewModel.getCachedUser()
        val initialIsAuthenticated = cachedUser != null && !authViewModel.isAuthExpired()
        if (!initialIsAuthenticated) {
            authViewModel.clearAuthCache()
        }

        _isAuthenticated.value = initialIsAuthenticated

        // Observe auth state changes
        scope.launch {
            authViewModel.authState.collect { state ->
                val authenticated = state is AuthState.Idle.Authenticated
                _isAuthenticated.value = authenticated
            }
        }
    }

    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        authViewModel.login(email, password, callback)
    }

    fun register(
        email: String,
        password: String,
        name: String,
        role: String,
        callback: (Boolean, String?) -> Unit
    ) {
        authViewModel.register(email, password, name, role, callback)
    }

    fun logout() {
        authViewModel.logout()
    }

    fun resetPassword(email: String, callback: (Boolean, String?) -> Unit) {
        authViewModel.resetPassword(email, callback)
    }

    fun clearAuthCache() {
        authViewModel.clearAuthCache()
    }
}
