package edu.utap.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import edu.utap.user.MainDispatcherRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class FirebaseStorageUtilTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var mockStorage: FirebaseStorage

    @MockK
    private lateinit var mockStorageRef: StorageReference

    @MockK
    private lateinit var mockProfileImagesRef: StorageReference

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockContentResolver: ContentResolver

    @MockK
    private lateinit var mockUri: Uri

    @MockK
    private lateinit var mockUploadTask: UploadTask

    @MockK
    private lateinit var mockUploadTaskSnapshot: UploadTask.TaskSnapshot

    private lateinit var storageUtil: FirebaseStorageUtil

    // Test data
    private val testUserId = "test-user-id"
    private val testDownloadUrl = "https://example.com/test-image.jpg"
    private val testMimeType = "image/jpeg"
    private val testFileExtension = "jpg"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        // Mock MimeTypeMap singleton method calls directly
        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton() } returns mockk {
            every { getExtensionFromMimeType(testMimeType) } returns testFileExtension
        }

        // Setup context and content resolver
        every { mockContext.contentResolver } returns mockContentResolver
        every { mockContentResolver.getType(mockUri) } returns testMimeType

        // Setup storage references
        every { mockStorage.reference } returns mockStorageRef
        every { mockStorageRef.child(any()) } returns mockProfileImagesRef

        // Setup upload task
        every { mockProfileImagesRef.putFile(mockUri) } returns mockUploadTask
        every { mockUploadTask.addOnSuccessListener(any()) } returns mockUploadTask
        every { mockUploadTask.addOnFailureListener(any()) } returns mockUploadTask

        // Setup download URL task
        val mockDownloadUrlTask = mockk<Task<Uri>>()
        every { mockProfileImagesRef.downloadUrl } returns mockDownloadUrlTask
        every { mockDownloadUrlTask.isComplete } returns true
        every { mockDownloadUrlTask.isSuccessful } returns true
        every { mockDownloadUrlTask.result } returns mockUri
        every { mockDownloadUrlTask.exception } returns null
        every { mockUri.toString() } returns testDownloadUrl

        storageUtil = FirebaseStorageUtil(mockStorage)
    }

    @Test
    fun `uploadProfileImage should upload file and return download URL on success`() = runTest {
        // Arrange
        // Mock the kotlinx.coroutines.tasks.await extension function
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        coEvery { mockUploadTask.await() } returns mockUploadTaskSnapshot

        val mockDownloadUrlTask = mockk<Task<Uri>>()
        every { mockProfileImagesRef.downloadUrl } returns mockDownloadUrlTask
        every { mockDownloadUrlTask.isComplete } returns true
        every { mockDownloadUrlTask.isSuccessful } returns true
        every { mockDownloadUrlTask.result } returns mockUri
        every { mockDownloadUrlTask.exception } returns null
        coEvery { mockDownloadUrlTask.await() } returns mockUri

        // Act
        val result = storageUtil.uploadProfileImage(mockContext, mockUri, testUserId)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(testDownloadUrl, (result as Result.Success).data)
        verify { mockProfileImagesRef.putFile(mockUri) }
        verify { mockProfileImagesRef.downloadUrl }

        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @Test
    fun `uploadProfileImage should return failure when upload fails`() = runTest {
        // Arrange
        val testException = Exception("Upload failed")

        // Mock the kotlinx.coroutines.tasks.await extension function
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        coEvery { mockUploadTask.await() } throws testException

        // Act
        val result = storageUtil.uploadProfileImage(mockContext, mockUri, testUserId)

        // Assert
        assertTrue(result is Result.Error)
        assertEquals(testException, (result as Result.Error).cause)
        verify { mockProfileImagesRef.putFile(mockUri) }

        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @Test
    fun `uploadProfileImage should return failure when getting download URL fails`() = runTest {
        // Arrange
        val testException = Exception("Failed to get download URL")

        // Mock the kotlinx.coroutines.tasks.await extension function
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        coEvery { mockUploadTask.await() } returns mockUploadTaskSnapshot

        val mockDownloadUrlTask = mockk<Task<Uri>>()
        every { mockProfileImagesRef.downloadUrl } returns mockDownloadUrlTask
        every { mockDownloadUrlTask.isComplete } returns true
        every { mockDownloadUrlTask.isSuccessful } returns false
        every { mockDownloadUrlTask.exception } returns testException
        coEvery { mockDownloadUrlTask.await() } throws testException

        // Act
        val result = storageUtil.uploadProfileImage(mockContext, mockUri, testUserId)

        // Assert
        assertTrue(result is Result.Error)
        assertEquals(testException, (result as Result.Error).cause)
        verify { mockProfileImagesRef.putFile(mockUri) }
        verify { mockProfileImagesRef.downloadUrl }

        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @Test
    fun `uploadProfileImage should use jpg as default extension when mime type is null`() =
        runTest {
            // Arrange
            every { mockContentResolver.getType(mockUri) } returns null

            // Mock the kotlinx.coroutines.tasks.await extension function
            mockkStatic("kotlinx.coroutines.tasks.TasksKt")
            coEvery { mockUploadTask.await() } returns mockUploadTaskSnapshot

            val mockDownloadUrlTask = mockk<Task<Uri>>()
            every { mockProfileImagesRef.downloadUrl } returns mockDownloadUrlTask
            every { mockDownloadUrlTask.isComplete } returns true
            every { mockDownloadUrlTask.isSuccessful } returns true
            every { mockDownloadUrlTask.result } returns mockUri
            every { mockDownloadUrlTask.exception } returns null
            coEvery { mockDownloadUrlTask.await() } returns mockUri

            // Act
            val result = storageUtil.uploadProfileImage(mockContext, mockUri, testUserId)

            // Assert
            assertTrue(result is Result.Success)
            verify { mockStorageRef.child(match { it.contains(".jpg") }) }

            unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
        }
    @Test
    fun `deleteProfileImage should delete file and return success on success`() = runTest {
        // Arrange
        val imageUrl = "https://example.com/test-image.jpg"
        val mockDeleteRef = mockk<StorageReference>()
        val mockDeleteTask = mockk<Task<Void>>()

        mockkStatic("kotlinx.coroutines.tasks.TasksKt")

        every { mockStorage.getReferenceFromUrl(imageUrl) } returns mockDeleteRef
        every { mockDeleteRef.delete() } returns mockDeleteTask
        coEvery { mockDeleteTask.await() } returns mockk()

        // Act
        val result = storageUtil.deleteProfileImage(imageUrl)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(Unit, (result as Result.Success).data)

        verify { mockStorage.getReferenceFromUrl(imageUrl) }
        verify { mockDeleteRef.delete() }

        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @Test
    fun `deleteProfileImage should return failure when delete fails`() = runTest {
        // Arrange
        val imageUrl = "https://example.com/test-image.jpg"
        val testException = Exception("Delete failed")
        val mockDeleteRef = mockk<StorageReference>()
        val mockDeleteTask = mockk<Task<Void>>()

        mockkStatic("kotlinx.coroutines.tasks.TasksKt")

        every { mockStorage.getReferenceFromUrl(imageUrl) } returns mockDeleteRef
        every { mockDeleteRef.delete() } returns mockDeleteTask
        coEvery { mockDeleteTask.await() } throws testException

        // Act
        val result = storageUtil.deleteProfileImage(imageUrl)

        // Assert
        assertTrue(result is Result.Error)
        assertEquals(testException, (result as Result.Error).cause)

        verify { mockStorage.getReferenceFromUrl(imageUrl) }
        verify { mockDeleteRef.delete() }

        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }
}
