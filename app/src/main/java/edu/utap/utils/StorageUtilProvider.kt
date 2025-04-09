package edu.utap.utils

import edu.utap.user.FirebaseStorageUtilInterface

interface StorageUtilProviderInterface {
    fun getStorageUtil(): FirebaseStorageUtilInterface
}

class DefaultStorageUtilProvider(
    private val storageUtil: FirebaseStorageUtilInterface
) : StorageUtilProviderInterface {
    override fun getStorageUtil(): FirebaseStorageUtilInterface = storageUtil
}
