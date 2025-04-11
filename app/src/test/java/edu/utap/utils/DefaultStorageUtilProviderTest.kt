package edu.utap.utils

import org.junit.Test

class DefaultStorageUtilProviderTest {

    @Test
    fun `getStorageUtil returns correct instance`() {
        // Check if the getStorageUtil() method returns the exact
        // FirebaseStorageUtilInterface instance that was provided during initialization.
        val fakeStorageUtil = object : FirebaseStorageUtilInterface {
            override suspend fun uploadProfileImage(context: android.content.Context, imageUri: android.net.Uri, userId: String) = throw NotImplementedError()
            override suspend fun deleteProfileImage(imageUrl: String) = throw NotImplementedError()
        }
        val provider = DefaultStorageUtilProvider(fakeStorageUtil)
        val result = provider.getStorageUtil()
        assert(result === fakeStorageUtil)
    }

    @Test
    fun `getStorageUtil returns non null value`() {
        // Verify that getStorageUtil() method does not return null
        // when initialized with a valid FirebaseStorageUtilInterface implementation.
        val fakeStorageUtil = object : FirebaseStorageUtilInterface {
            override suspend fun uploadProfileImage(context: android.content.Context, imageUri: android.net.Uri, userId: String) = throw NotImplementedError()
            override suspend fun deleteProfileImage(imageUrl: String) = throw NotImplementedError()
        }
        val provider = DefaultStorageUtilProvider(fakeStorageUtil)
        val result = provider.getStorageUtil()
        assert(result != null)
    }

    @Test
    fun `getStorageUtil multiple calls`() {
        // Check if calling getStorageUtil() multiple times returns
        // the same FirebaseStorageUtilInterface instance each time.
        val fakeStorageUtil = object : FirebaseStorageUtilInterface {
            override suspend fun uploadProfileImage(context: android.content.Context, imageUri: android.net.Uri, userId: String) = throw NotImplementedError()
            override suspend fun deleteProfileImage(imageUrl: String) = throw NotImplementedError()
        }
        val provider = DefaultStorageUtilProvider(fakeStorageUtil)
        val result1 = provider.getStorageUtil()
        val result2 = provider.getStorageUtil()
        assert(result1 === result2)
    }

    @Test
    fun `getStorageUtil after different interactions`() {
        // Check if getting storage util after performing some action on another part of the code,
        // that it returns the correct instance.
        val fakeStorageUtil = object : FirebaseStorageUtilInterface {
            override suspend fun uploadProfileImage(context: android.content.Context, imageUri: android.net.Uri, userId: String) = throw NotImplementedError()
            override suspend fun deleteProfileImage(imageUrl: String) = throw NotImplementedError()
        }
        val provider = DefaultStorageUtilProvider(fakeStorageUtil)
        // Simulate unrelated action
        val unrelatedList = mutableListOf(1, 2, 3)
        unrelatedList.add(4)
        // Now get storage util
        val result = provider.getStorageUtil()
        assert(result === fakeStorageUtil)
    }

    @Test
    fun `getStorageUtil with mocked instance`() {
        // Create a mocked FirebaseStorageUtilInterface and use
        // it during initialization. Then verify that getStorageUtil() returns the mocked object.
        val mockStorageUtil = io.mockk.mockk<FirebaseStorageUtilInterface>()
        val provider = DefaultStorageUtilProvider(mockStorageUtil)
        val result = provider.getStorageUtil()
        assert(result === mockStorageUtil)
    }

    @Test
    fun `check class type`() {
        // Verify that the returned object of getStorageUtil() is the correct type
        val fakeStorageUtil = object : FirebaseStorageUtilInterface {
            override suspend fun uploadProfileImage(context: android.content.Context, imageUri: android.net.Uri, userId: String) = throw NotImplementedError()
            override suspend fun deleteProfileImage(imageUrl: String) = throw NotImplementedError()
        }
        val provider = DefaultStorageUtilProvider(fakeStorageUtil)
        val result = provider.getStorageUtil()
        assert(result is FirebaseStorageUtilInterface)
    }

}
