package edu.utap.auth.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.auth.FirestoreAuthRepository
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.user.repository.FirebaseUserRepository

/**
 * A simple dependency provider for the AuthRepository
 * This could be replaced with Hilt/Dagger in a more complex application
 */
/**
 * Simple singleton dependency provider for the authentication repository.
 *
 * This avoids tight coupling and allows for easier testing or replacement.
 * In a production app, this would typically be replaced by a DI framework like Hilt or Dagger.
 */
object AuthModule {

    private var authRepository: AuthRepositoryInterface? = null

    /**
     * Provides a singleton instance of [AuthRepositoryInterface].
     *
     * Lazily initializes the repository with FirebaseAuth, Firestore, and UserRepository.
     * Thread-safe via `synchronized`.
     *
     * @return The singleton [AuthRepositoryInterface] implementation.
     */
    fun provideAuthRepository(): AuthRepositoryInterface =
        authRepository ?: synchronized(this) {
            val firebaseAuth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val userRepository = FirebaseUserRepository()

            FirestoreAuthRepository(firebaseAuth, firestore, userRepository).also {
                authRepository = it
            }
        }
}
