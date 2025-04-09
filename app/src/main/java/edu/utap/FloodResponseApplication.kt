package edu.utap

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.auth.di.AuthModule
import edu.utap.user.FirebaseStorageUtil
import edu.utap.user.repository.FirebaseUserRepository
import edu.utap.utils.*

/**
 * Custom [Application] class serving as a manual DI container.
 */
class FloodResponseApplication : Application() {

    lateinit var appContextProvider: ApplicationContextProviderInterface
        private set

    lateinit var securePrefsProvider: SecurePrefsProviderInterface
        private set

    lateinit var storageUtilProvider: StorageUtilProviderInterface
        private set

    lateinit var networkUtils: NetworkUtilsInterface
        private set

    lateinit var authModule: AuthModule
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize providers
        appContextProvider = DefaultApplicationContextProvider(applicationContext)

        securePrefsProvider = DefaultSecurePrefsProvider(applicationContext)

        storageUtilProvider = DefaultStorageUtilProvider(FirebaseStorageUtil())

        networkUtils = NetworkUtilsImpl()

        val firebaseAuth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        var userRepository = FirebaseUserRepository()
        userRepository = FirebaseUserRepository(firebaseAuth, firestore)

        authModule = AuthModule(firebaseAuth, firestore)
    }
}
