package edu.utap.user

import com.google.firebase.auth.FirebaseUser

/**
 * Sealed class representing the different states of user profile operations.
 * 
 * This provides a type-safe way to handle the various states that can occur
 * during user profile operations (loading, success, error, etc.).
 */
sealed class UserProfileState {
    /**
     * Base class for states where no profile operation is in progress
     */
    sealed class Idle : UserProfileState() {
        /**
         * Initial state when no profile operations have been performed
         */
        data object Initial : Idle()
        
        /**
         * State when profile data has been successfully loaded
         */
        data class Loaded(val profile: UserProfile) : Idle()
        
        /**
         * State when profile has been successfully updated
         */
        data class Updated(val profile: UserProfile) : Idle()
    }
    
    /**
     * Base class for states where a profile operation is in progress
     */
    sealed class Loading : UserProfileState() {
        /**
         * Loading state for profile fetch operation
         */
        data object Fetching : Loading()
        
        /**
         * Loading state for profile update operation
         */
        data object Updating : Loading()

        /**
         * Loading state for profile creation operation
         */
        data object Creating : Loading()
    }
    
    /**
     * Base class for states where a profile operation resulted in an error
     */
    sealed class Error : UserProfileState() {
        /**
         * Generic error state with a message
         */
        data class Generic(val message: String) : Error()
        
        /**
         * Network-related error state
         */
        data class Network(val message: String) : Error()
        
        /**
         * Validation error state (invalid data format, etc.)
         */
        data class Validation(val message: String) : Error()
    }

    /**
     * State representing a successful profile operation
     */
    data class Success(val profile: UserProfile) : UserProfileState()
}