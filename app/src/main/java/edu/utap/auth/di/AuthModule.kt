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
object AuthModule {

    private var authRepository: AuthRepositoryInterface? = null

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
