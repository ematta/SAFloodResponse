package edu.utap.utils

/**
 * Interface for providing a Firebase Storage utility instance.
 *
 * This interface defines a contract for classes that can provide
 * an instance of [FirebaseStorageUtilInterface], which is responsible
 * for interacting with Firebase Storage.
 *
 * Implementations of this interface are typically used to abstract
 * the creation and management of the storage utility, allowing for
 * dependency injection and easier testing.
 */
interface StorageUtilProviderInterface {
    fun getStorageUtil(): FirebaseStorageUtilInterface
}

/**
 * Default implementation of [StorageUtilProviderInterface] that provides a pre-configured
 * instance of [FirebaseStorageUtilInterface].
 *
 * This class acts as a simple provider for accessing a Firebase Storage utility. It takes
 * an instance of [FirebaseStorageUtilInterface] during construction and returns the same
 * instance when requested through [getStorageUtil].
 *
 * @property storageUtil The instance of [FirebaseStorageUtilInterface] to be provided.
 *                      This is typically a concrete implementation of the storage utility,
 *                      like one interacting directly with Firebase Storage.
 *
 * Example usage:
 * ```kotlin
 * val firebaseStorage = FirebaseStorage.getInstance()
 * val firebaseStorageUtil = FirebaseStorageUtil(firebaseStorage)
 * val storageUtilProvider = DefaultStorageUtilProvider(firebaseStorageUtil)
 * val storageUtil = storageUtilProvider.getStorageUtil()
 * // Use storageUtil to interact with Firebase Storage
 * ```
 */
class DefaultStorageUtilProvider(private val storageUtil: FirebaseStorageUtilInterface) :
    StorageUtilProviderInterface {
    override fun getStorageUtil(): FirebaseStorageUtilInterface = storageUtil
}
