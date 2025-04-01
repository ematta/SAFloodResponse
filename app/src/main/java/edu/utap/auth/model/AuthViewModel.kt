package edu.utap.auth.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import edu.utap.auth.AuthState
import edu.utap.auth.AuthStateManager
import edu.utap.auth.NetworkOperationHandler
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.utils.FirebaseErrorMapper
import edu.utap.utils.NetworkUtilsInterface
import edu.utap.utils.NetworkUtilsProvider
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

    override var authState: StateFlow<AuthState> = stateManager.authState

    val currentUser: StateFlow<FirestoreUser?> = stateManager.currentUser

    override fun getCurrentUser(): FirestoreUser? = stateManager.currentUser.value as FirestoreUser?

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
    fun checkAuthState() {
        viewModelScope.launch {
            val firebaseUser = authRepository.getCurrentUser()
            if (firebaseUser != null) {
                stateManager.updateState(AuthState.Idle.Authenticated(firebaseUser))
                val userResult = authRepository.getUserById(firebaseUser.uid)
                userResult.onSuccess { firestoreUser ->
                    // Firebase user to FirestoreUser
                    val updatedUser = firestoreUser.copy()
                    authRepository.updateUser(updatedUser)
                    stateManager.updateCurrentUser(updatedUser)
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
    override fun register(
        email: String,
        password: String,
        name: String,
        role: String,
        function: (Boolean, String?) -> Unit
    ) {
        stateManager.updateState(AuthState.Loading.Registration)

        viewModelScope.launch {
            networkHandler.executeWithNetworkCheck {
                authRepository.registerUser(email, password, name, role)
            }.fold(
                onSuccess = { result ->
                    result.fold(
                        onSuccess = { user ->
                            stateManager.updateState(AuthState.Idle.Authenticated(user))
                            val userResult = authRepository.getUserById(user.uid)
                            userResult.onSuccess { firestoreUser ->
                                if (firestoreUser.role != role) {
                                    val updatedUser = firestoreUser.copy(role = role)
                                    authRepository.updateUser(updatedUser)
                                    stateManager.updateCurrentUser(updatedUser)
                                } else {
                                    stateManager.updateCurrentUser(firestoreUser)
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
        function(true, null)
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
    override fun login(email: String, password: String, function: (Boolean, String?) -> Unit) {
        stateManager.updateState(AuthState.Loading.Login)

        viewModelScope.launch {
            networkHandler.executeWithNetworkCheck {
                authRepository.loginUser(email, password)
            }.fold(
                onSuccess = { result ->
                    result.fold(
                        onSuccess = { user ->
                            stateManager.updateState(AuthState.Idle.Authenticated(user))
                            function(true, null)
                            val userResult = authRepository.getUserById(user.uid)
                            userResult.onSuccess { firestoreUser ->
                                stateManager.updateCurrentUser(firestoreUser)
                            }
                        },
                        onFailure = { error ->
                            val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                            function(false, errorMessage)
                            stateManager.updateState(AuthState.Error.Authentication(errorMessage))
                        }
                    )
                },
                onFailure = { error ->
                    val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                    function(false, errorMessage)
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
            try {
                authRepository.logout()
                stateManager.resetState()
            } catch (e: Exception) {
                val errorMessage = FirebaseErrorMapper.getErrorMessage(e)
                stateManager.updateState(AuthState.Error.Authentication(errorMessage))
            }
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
    override fun resetPassword(email: String, callback: (Boolean, String?) -> Unit) {
        stateManager.updateState(AuthState.Loading.PasswordReset)

        viewModelScope.launch {
            networkHandler.executeWithNetworkCheck {
                authRepository.resetPassword(email)
            }.fold(
                onSuccess = { result ->
                    result.fold(
                        onSuccess = {
                            stateManager.updateState(AuthState.Idle.PasswordResetSent)
                            callback(true, null)
                        },
                        onFailure = { error ->
                            val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                            stateManager.updateState(AuthState.Error.Authentication(errorMessage))
                            callback(false, errorMessage)
                        }
                    )
                },
                onFailure = { error ->
                    val errorMessage = FirebaseErrorMapper.getErrorMessage(error)
                    stateManager.updateState(AuthState.Error.Network(errorMessage))
                    callback(false, errorMessage)
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
    fun updateUserRole(userId: String, newRole: String): Result<FirestoreUser> = try {
        var result: Result<FirestoreUser> = Result.failure(Exception("Role update failed"))

        viewModelScope.launch {
            val userResult = authRepository.getUserById(userId)
            userResult.onSuccess { firestoreUser ->
                if (firestoreUser.role != newRole) {
                    val updatedUser = firestoreUser.copy(role = newRole)
                    result = authRepository.updateUser(updatedUser)

                    // If this is the current user, update the currentUser flow
                    if (userId == stateManager.currentUser.value?.userId) {
                        stateManager.updateCurrentUser(updatedUser)
                    }
                } else {
                    // Role already set to requested value
                    result = Result.success(firestoreUser)
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
        // TODO: Implement this method
        return true
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
    fun observeAllUsers() = authRepository.observeUsers()

    override fun restoreAuthState() {
        checkAuthState()
    }

    override fun updateAuthState(sent: AuthState) {
        stateManager.updateState(sent)
    }
}
