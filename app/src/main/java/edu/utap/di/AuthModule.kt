package edu.utap.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.repository.FirestoreAuthRepository
import edu.utap.repository.AuthRepositoryInterface

/**
 * Factory class for providing AuthRepository instances.
 * Dependencies are injected manually without relying on singletons.
 */
class AuthModule(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {
    fun provideAuthRepository(): AuthRepositoryInterface {
        return FirestoreAuthRepository(firebaseAuth, firestore)
    }
}
