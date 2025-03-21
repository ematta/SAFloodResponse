package edu.utap.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.utap.user.FirebaseUserRepository
import edu.utap.user.UserProfile
import edu.utap.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userRepository: UserRepository = FirebaseUserRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val currentUser = authRepository.getCurrentUser()
        _authState.value = if (currentUser != null) {
            AuthState.Authenticated(currentUser)
        } else {
            AuthState.Unauthenticated
        }
    }

    fun register(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.registerUser(email, password)
            result.fold(
                onSuccess = { user ->
                    // Create a basic user profile after successful registration
                    val userProfile = UserProfile(
                        uid = user.uid,
                        email = user.email ?: email,
                        displayName = user.displayName ?: ""
                    )
                    userRepository.createUserProfile(userProfile)
                    
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
        authRepository.logout()
        _authState.value = AuthState.Unauthenticated
    }
} 