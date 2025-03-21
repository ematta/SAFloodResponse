package edu.utap.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.auth.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepositoryInterface,
    private val context: Context
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            _authState.value = if (currentUser != null) {
                AuthState.Authenticated(currentUser)
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    fun register(email: String, password: String, name: String) {
        _authState.value = AuthState.Loading
        
        // Check for network connectivity before attempting registration
        if (!NetworkUtils.isNetworkAvailable(context)) {
            _authState.value = AuthState.Error("No internet connection. Please check your network settings and try again.")
            return
        }
        
        viewModelScope.launch {
            val result = authRepository.registerUser(email, password, name)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Registration failed")
                }
            )
        }
    }

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        
        // Check for network connectivity before attempting login
        if (!NetworkUtils.isNetworkAvailable(context)) {
            _authState.value = AuthState.Error("No internet connection. Please check your network settings and try again.")
            return
        }
        
        viewModelScope.launch {
            val result = authRepository.loginUser(email, password)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Unauthenticated
        }
    }
}