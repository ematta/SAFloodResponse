package edu.utap.utils

interface StorageUtilProviderInterface {
    fun getStorageUtil(): FirebaseStorageUtilInterface
}

class DefaultStorageUtilProvider(private val storageUtil: FirebaseStorageUtilInterface) :
    StorageUtilProviderInterface {
    override fun getStorageUtil(): FirebaseStorageUtilInterface = storageUtil
}
