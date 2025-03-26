package edu.utap.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.utap.auth.db.UserEntity
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.auth.utils.FirebaseErrorMapper
import edu.utap.auth.utils.NetworkUtilsInterface
import edu.utap.auth.utils.NetworkUtilsProvider
import edu.utap.auth.utils.ApplicationContextProvider
import edu.utap.auth.utils.RoleUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing authentication state and operations.
 * 
 * This ViewModel implements the AuthViewModelInterface to provide a consistent API
 * for authentication operations across the application. It handles:
 * - User registration
 * - User login
 * - Password reset
 * - Logout
 * - Authentication state management
 * - Role-based access control
 * 
 * It also includes network connectivity checks before attempting operations
 * that require internet access.
 */
open class AuthViewModel(
    private val authRepository: AuthRepositoryInterface,
    private val networkUtils: NetworkUtilsInterface = NetworkUtilsProvider.getNetworkUtils()
) : ViewModel(), AuthViewModelInterface {

    // StateFlow to expose authentication state to UI components
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle.Initial)
    override val authState: StateFlow<AuthState> = _authState
    
    // StateFlow to expose the current user entity
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    init {
        // Check authentication state when ViewModel is created
        checkAuthState()
    }

    /**
     * Checks the current authentication state and updates the authState flow.
     * 
     * This method queries the repository to determine if a user is currently
     * authenticated, and updates the authState flow accordingly.
     * UI components observing this flow will automatically react to state changes.
     */
    private fun checkAuthState() {
        viewModelScope.launch {
            val firebaseUser = authRepository.getCurrentUser()
            if (firebaseUser != null) {
                // User is authenticated
                _authState.value = AuthState.Idle.Authenticated(firebaseUser)
                
                // Fetch and update the current user entity
                val userResult = authRepository.getLocalUserById(firebaseUser.uid)
                userResult.onSuccess { userEntity ->
                    _currentUser.value = userEntity
                }
            } else {
                // User is not authenticated
                _authState.value = AuthState.Idle.Unauthenticated
                _currentUser.value = null
            }
        }
    }

    /**
     * Registers a new user with the provided credentials.
     * 
     * This method performs the following steps:
     * 1. Updates the auth state to Loading
     * 2. Checks for network connectivity
     * 3. Calls the repository to register the user
     * 4. Updates the auth state based on the result
     * 
     * Network connectivity is checked before attempting registration to provide
     * immediate feedback to the user if they're offline.
     * 
     * @param email User's email address
     * @param password User's password
     * @param name User's display name
     * @param role User's role (defaults to regular user)
     */
    override fun register(
        email: String,
        password: String,
        name: String,
        role: String
    ) {
        // Update state to indicate loading
        _authState.value = AuthState.Loading.Registration
        
        // Check for network connectivity before attempting registration
        // This prevents unnecessary API calls and provides immediate feedback
        if (!networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
            _authState.value = AuthState.Error.Network("No internet connection. Please check your network settings and try again.")
            return
        }
        
        viewModelScope.launch {
            val result = authRepository.registerUser(email, password, name)
            result.fold(
                onSuccess = { user ->
                    // Registration successful, update state to authenticated
                    _authState.value = AuthState.Idle.Authenticated(user)
                    
                    // Fetch and update the current user entity
                    val userResult = authRepository.getLocalUserById(user.uid)
                    userResult.onSuccess { userEntity ->
                        // Update user role if needed
                        if (userEntity.role != role) {
                            val updatedUser = userEntity.copy(role = role)
                            authRepository.updateLocalUser(updatedUser)
                            _currentUser.value = updatedUser
                        } else {
                            _currentUser.value = userEntity
                        }
                    }
                },
                onFailure = { error ->
                    // Registration failed, map error to user-friendly message
                    val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                    _authState.value = AuthState.Error.Validation(errorMessage)
                }
            )
        }
    }

    /**
     * Authenticates a user with the provided credentials.
     * 
     * This method performs the following steps:
     * 1. Updates the auth state to Loading
     * 2. Checks for network connectivity
     * 3. Calls the repository to authenticate the user
     * 4. Updates the auth state based on the result
     * 
     * Network connectivity is checked before attempting login to provide
     * immediate feedback to the user if they're offline.
     * 
     * @param email User's email address
     * @param password User's password
     */
    override fun login(email: String, password: String) {
        // Update state to indicate loading
        _authState.value = AuthState.Loading.Login
        
        // Check for network connectivity before attempting login
        // This prevents unnecessary API calls and provides immediate feedback
        if (!networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
            _authState.value = AuthState.Error.Network("No internet connection. Please check your network settings and try again.")
            return
        }
        
        viewModelScope.launch {
            val result = authRepository.loginUser(email, password)
            result.fold(
                onSuccess = { user ->
                    // Login successful, update state to authenticated
                    _authState.value = AuthState.Idle.Authenticated(user)
                    
                    // Fetch and update the current user entity
                    val userResult = authRepository.getLocalUserById(user.uid)
                    userResult.onSuccess { userEntity ->
                        // Ensure we're properly handling the UserEntity type
                        if (userEntity is UserEntity) {
                            _currentUser.value = userEntity
                        }
                    }
                },
                onFailure = { error ->
                    // Login failed, map error to user-friendly message
                    val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                    _authState.value = AuthState.Error.Authentication(errorMessage)
                }
            )
        }
    }

    /**
     * Logs out the current user.
     * 
     * This method calls the repository to sign out the user and updates
     * the authentication state to Unauthenticated.
     * 
     * Unlike other authentication operations, logout doesn't require
     * network connectivity as it primarily clears local session data.
     */
    override fun logout() {
        viewModelScope.launch {
            // Sign out the user through the repository
            authRepository.logout()
            // Update state to unauthenticated
            _authState.value = AuthState.Idle.Unauthenticated
            _currentUser.value = null
        }
    }

    /**
     * Sends a password reset email to the provided email address.
     * 
     * This method performs the following steps:
     * 1. Updates the auth state to Loading
     * 2. Checks for network connectivity
     * 3. Calls the repository to send a password reset email
     * 4. Updates the auth state based on the result
     * 
     * Network connectivity is checked before attempting password reset to provide
     * immediate feedback to the user if they're offline.
     * 
     * @param email The email address to send the password reset link to
     */
    override fun resetPassword(email: String) {
        // Update state to indicate loading
        _authState.value = AuthState.Loading.PasswordReset
        
        // Check for network connectivity before attempting password reset
        // This prevents unnecessary API calls and provides immediate feedback
        if (!networkUtils.isNetworkAvailable(ApplicationContextProvider.getApplicationContext())) {
            _authState.value = AuthState.Error.Network("No internet connection. Please check your network settings and try again.")
            return
        }
        
        viewModelScope.launch {
            val result = authRepository.resetPassword(email)
            result.fold(
                onSuccess = {
                    // Password reset email sent successfully
                    _authState.value = AuthState.Idle.PasswordResetSent
                },
                onFailure = { error ->
                    // Password reset failed, map error to user-friendly message
                    val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                    _authState.value = AuthState.Error.Authentication(errorMessage)
                }
            )
        }
    }
    
    /**
     * Updates the role of a user.
     * 
     * This method allows changing a user's role, which affects their permissions 
     * throughout the application.
     * 
     * Note: This should typically only be called by admin users.
     * 
     * @param userId The ID of the user to update
     * @param newRole The new role to assign to the user
     * @return Result indicating success or failure of the operation
     */
    fun updateUserRole(userId: String, newRole: String): Result<UserEntity> {
        return try {
            var result: Result<UserEntity> = Result.failure(Exception("Role update failed"))
            
            viewModelScope.launch {
                val userResult = authRepository.getLocalUserById(userId)
                userResult.onSuccess { userEntity ->
                    if (userEntity.role != newRole) {
                        val updatedUser = userEntity.copy(role = newRole)
                        result = authRepository.updateLocalUser(updatedUser)
                        
                        // If this is the current user, update the currentUser flow
                        if (userId == _currentUser.value?.userId) {
                            _currentUser.value = updatedUser
                        }
                    } else {
                        // Role already set to requested value
                        result = Result.success(userEntity)
                    }
                }
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Checks if the current user has a specific permission.
     * 
     * @param requiredRole The minimum role required for the permission
     * @return true if the user has the required permission, false otherwise
     */
    fun hasPermission(requiredRole: String): Boolean {
        val user = _currentUser.value ?: return false
        return RoleUtils.hasPermission(user.role, requiredRole)
    }
    
    /**
     * Observes all users in the system.
     * 
     * This method returns a Flow of all UserEntity objects in the database,
     * which can be collected to display user lists or perform batch operations.
     * 
     * Note: This should typically only be accessed by admin users.
     * 
     * @return Flow of all users in the system
     */
    fun observeAllUsers() = authRepository.observeLocalUsers()
}