package edu.utap.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.auth.utils.FirebaseErrorMapper
import edu.utap.auth.utils.NetworkUtilsInterface
import edu.utap.auth.utils.NetworkUtilsProvider
import edu.utap.auth.utils.ApplicationContextProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class AuthViewModel(
    private val authRepository: AuthRepositoryInterface,
    private val networkUtils: NetworkUtilsInterface = NetworkUtilsProvider.getNetworkUtils()
) : ViewModel(), AuthViewModelInterface {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    override val authState: StateFlow<AuthState> = _authState

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

    override fun register(email: String, password: String, name: String) {
        _authState.value = AuthState.Loading
        
        // Check for network connectivity before attempting registration
        if (!networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
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
                    val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                    _authState.value = AuthState.Error(errorMessage)
                }
            )
        }
    }

    override fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        
        // Check for network connectivity before attempting login
        if (!networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
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
                    val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                    _authState.value = AuthState.Error(errorMessage)
                }
            )
        }
    }

    override fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Unauthenticated
        }
    }

    override fun resetPassword(email: String) {
        _authState.value = AuthState.Loading
        
        // Check for network connectivity before attempting password reset
        if (!networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
            _authState.value = AuthState.Error("No internet connection. Please check your network settings and try again.")
            return
        }
        
        viewModelScope.launch {
            val result = authRepository.resetPassword(email)
            result.fold(
                onSuccess = {
                    _authState.value = AuthState.PasswordResetSent
                },
                onFailure = { error ->
                    val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                    _authState.value = AuthState.Error(errorMessage)
                }
            )
        }
    }
}