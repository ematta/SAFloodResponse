package edu.utap.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.utap.auth.db.UserEntity
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.auth.utils.FirebaseErrorMapper
import edu.utap.auth.utils.NetworkUtilsInterface
import edu.utap.auth.utils.NetworkUtilsProvider
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
) : ViewModel(),
    AuthViewModelInterface {

    private val stateManager = AuthStateManager()
    private val networkHandler = NetworkOperationHandler(networkUtils)

    override val authState: StateFlow<AuthState> = stateManager.authState
    val currentUser: StateFlow<UserEntity?> = stateManager.currentUser

    override fun getCurrentUser(): UserEntity? {
        return stateManager.currentUser.value
    }

    init {
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
                stateManager.updateState(AuthState.Idle.Authenticated(firebaseUser))
                val userResult = authRepository.getLocalUserById(firebaseUser.uid)
                userResult.onSuccess { userEntity ->
                    stateManager.updateCurrentUser(userEntity)
                }
            } else {
                stateManager.resetState()
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
    override fun register(email: String, password: String, name: String, role: String) {
        stateManager.updateState(AuthState.Loading.Registration)

        viewModelScope.launch {
            networkHandler.executeWithNetworkCheck {
                authRepository.registerUser(email, password, name)
            }.fold(
                onSuccess = { result ->
                    result.fold(
                        onSuccess = { user ->
                            stateManager.updateState(AuthState.Idle.Authenticated(user))
                            val userResult = authRepository.getLocalUserById(user.uid)
                            userResult.onSuccess { userEntity ->
                                if (userEntity.role != role) {
                                    val updatedUser = userEntity.copy(role = role)
                                    authRepository.updateLocalUser(updatedUser)
                                    stateManager.updateCurrentUser(updatedUser)
                                } else {
                                    stateManager.updateCurrentUser(userEntity)
                                }
                            }
                        },
                        onFailure = { error ->
                            val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                            stateManager.updateState(AuthState.Error.Validation(errorMessage))
                        }
                    )
                },
                onFailure = { error ->
                    val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                    stateManager.updateState(AuthState.Error.Validation(errorMessage))
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
        stateManager.updateState(AuthState.Loading.Login)

        viewModelScope.launch {
            networkHandler.executeWithNetworkCheck {
                authRepository.loginUser(email, password)
            }.fold(
                onSuccess = { result ->
                    result.fold(
                        onSuccess = { user ->
                            stateManager.updateState(AuthState.Idle.Authenticated(user))
                            val userResult = authRepository.getLocalUserById(user.uid)
                            userResult.onSuccess { userEntity ->
                                stateManager.updateCurrentUser(userEntity)
                            }
                        },
                        onFailure = { error ->
                            val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                            stateManager.updateState(AuthState.Error.Authentication(errorMessage))
                        }
                    )
                },
                onFailure = { error ->
                    val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                    stateManager.updateState(AuthState.Error.Authentication(errorMessage))
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
            authRepository.logout()
            stateManager.resetState()
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
        stateManager.updateState(AuthState.Loading.PasswordReset)

        viewModelScope.launch {
            networkHandler.executeWithNetworkCheck {
                authRepository.resetPassword(email)
            }.fold(
                onSuccess = { result ->
                    result.fold(
                        onSuccess = {
                            stateManager.updateState(AuthState.Idle.PasswordResetSent)
                        },
                        onFailure = { error ->
                            val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                            stateManager.updateState(AuthState.Error.Authentication(errorMessage))
                        }
                    )
                },
                onFailure = { error ->
                    val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                    stateManager.updateState(AuthState.Error.Network(errorMessage))
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
    fun updateUserRole(userId: String, newRole: String): Result<UserEntity> = try {
        var result: Result<UserEntity> = Result.failure(Exception("Role update failed"))

        viewModelScope.launch {
            val userResult = authRepository.getLocalUserById(userId)
            userResult.onSuccess { userEntity ->
                if (userEntity.role != newRole) {
                    val updatedUser = userEntity.copy(role = newRole)
                    result = authRepository.updateLocalUser(updatedUser)

                    // If this is the current user, update the currentUser flow
                    if (userId == stateManager.currentUser.value?.userId) {
                        stateManager.updateCurrentUser(updatedUser)
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

    /**
     * Checks if the current user has a specific permission.
     *
     * @param requiredRole The minimum role required for the permission
     * @return true if the user has the required permission, false otherwise
     */
    fun hasPermission(requiredRole: String): Boolean {
        val user = stateManager.currentUser.value ?: return false
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
