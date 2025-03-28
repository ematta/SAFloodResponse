package edu.utap.auth.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import edu.utap.auth.AuthRepository
import edu.utap.db.AppDatabase
import edu.utap.auth.repository.AuthRepositoryInterface

/**
 * A simple dependency provider for the AuthRepository
 * This could be replaced with Hilt/Dagger in a more complex application
 */
object AuthModule {

    private var authRepository: AuthRepositoryInterface? = null

    fun provideAuthRepository(context: Context): AuthRepositoryInterface =
        authRepository ?: synchronized(this) {
            val database = AppDatabase.getDatabase(context)
            val userDao = database.userDao()
            val firebaseAuth = FirebaseAuth.getInstance()

            AuthRepository(firebaseAuth, userDao).also {
                authRepository = it
            }
        }
}
