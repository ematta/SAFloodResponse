package edu.utap.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.auth.FirestoreAuthRepository
import edu.utap.repository.AuthRepositoryInterface

/**
 * Factory class for providing AuthRepository instances.
 * Dependencies are injected manually without relying on singletons.
 */
class AuthModule(private val firebaseAuth: FirebaseAuth, private val firestore: FirebaseFirestore) {
    /**
     * Provides an instance of the [AuthRepositoryInterface] for handling authentication-related operations.
     *
     * This function creates and returns a [FirestoreAuthRepository] instance, which interacts with Firebase
     * Authentication ([FirebaseAuth]) and Firestore ([FirebaseFirestore]) to perform actions like user
     * registration, login, logout, and fetching user data.
     *
     * @param firebaseAuth An instance of [FirebaseAuth] used for interacting with Firebase Authentication.
     * @param firestore An instance of [FirebaseFirestore] used for interacting with Firestore.
     * @return An instance of [AuthRepositoryInterface], specifically a [FirestoreAuthRepository].
     *
     * @see AuthRepositoryInterface
     * @see FirestoreAuthRepository
     * @see FirebaseAuth
     * @see FirebaseFirestore
     */
    fun provideAuthRepository(): AuthRepositoryInterface =
        FirestoreAuthRepository(firebaseAuth, firestore)
}
