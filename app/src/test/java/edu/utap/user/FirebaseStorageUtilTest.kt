// package edu.utap.user

// import android.content.Context
// import android.net.Uri
// import android.webkit.MimeTypeMap
// import com.google.android.gms.tasks.Task
// import com.google.android.gms.tasks.Tasks
// import com.google.firebase.storage.FirebaseStorage
// import com.google.firebase.storage.StorageReference
// import com.google.firebase.storage.UploadTask
// import kotlinx.coroutines.ExperimentalCoroutinesApi
// import kotlinx.coroutines.tasks.await
// import kotlinx.coroutines.test.runTest
// import org.junit.After
// import org.junit.Before
// import org.junit.Rule
// import org.junit.Test
// import org.mockito.ArgumentMatchers.*
// import org.mockito.Mock
// import org.mockito.Mockito
// import org.mockito.MockitoAnnotations
// import org.mockito.MockedStatic
// import org.mockito.Mockito.mockStatic
// import kotlin.test.assertEquals
// import kotlin.test.assertTrue

// @OptIn(ExperimentalCoroutinesApi::class)
// class FirebaseStorageUtilTest {
    
//     @get:Rule
//     val mainDispatcherRule = MainDispatcherRule()
    
//     // Mock dependencies
//     @Mock
//     private lateinit var mockStorage: FirebaseStorage
    
//     @Mock
//     private lateinit var mockStorageRef: StorageReference
    
//     @Mock
//     private lateinit var mockProfileImagesRef: StorageReference
    
//     @Mock
//     private lateinit var mockContext: Context
    
//     @Mock
//     private lateinit var mockUri: Uri
    
//     @Mock
//     private lateinit var mockContentResolver: android.content.ContentResolver
    
//     @Mock
//     private lateinit var mockMimeTypeMap: MimeTypeMap
    
//     // Mock static methods
//     private lateinit var mockedMimeTypeMapStatic: MockedStatic<MimeTypeMap>
    
//     // Class under test
//     private lateinit var firebaseStorageUtil: FirebaseStorageUtil
    
//     // Test data
//     private val testUserId = "test-user-id"
//     private val testDownloadUrl = "https://example.com/test-image.jpg"
//     private val testFileExtension = "jpg"
//     private val testMimeType = "image/jpeg"
    
//     @Before
//     fun setup() {
//         MockitoAnnotations.openMocks(this)
        
//         // Mock static MimeTypeMap.getSingleton() method
//         mockedMimeTypeMapStatic = mockStatic(MimeTypeMap::class.java)
//         mockedMimeTypeMapStatic.`when`<MimeTypeMap> { MimeTypeMap.getSingleton() }.thenReturn(mockMimeTypeMap)
        
//         // Setup mocks for getFileExtension
//         Mockito.`when`(mockContext.contentResolver).thenReturn(mockContentResolver)
//         Mockito.`when`(mockContentResolver.getType(mockUri)).thenReturn(testMimeType)
//         Mockito.`when`(mockMimeTypeMap.getExtensionFromMimeType(testMimeType)).thenReturn(testFileExtension)
        
//         // Setup mocks for uploadProfileImage
//         Mockito.`when`(mockStorage.reference).thenReturn(mockStorageRef)
//         Mockito.`when`(mockStorageRef.child(anyString())).thenReturn(mockProfileImagesRef)
        
//         // Create a FirebaseStorageUtil with mocked dependencies
//         firebaseStorageUtil = FirebaseStorageUtil(mockStorage)
//     }
    
//     @Test
//     fun testUploadProfileImageSuccess() = runTest {
//         // Arrange
//         val mockUploadTask = Mockito.mock(UploadTask::class.java)
//         val mockTaskSnapshot = Mockito.mock(UploadTask.TaskSnapshot::class.java)
//         val mockUriResult = Mockito.mock(Uri::class.java)
//         val mockDownloadUrlTask: Task<Uri> = Tasks.forResult(mockUriResult)
        
//         Mockito.`when`(mockProfileImagesRef.putFile(mockUri)).thenReturn(mockUploadTask)
//         Mockito.`when`(mockUploadTask.await()).thenReturn(mockTaskSnapshot)
//         Mockito.`when`(mockUploadTask.addOnSuccessListener(any())).thenReturn(mockUploadTask)
//         Mockito.`when`(mockUploadTask.addOnFailureListener(any())).thenReturn(mockUploadTask)
//         Mockito.`when`(mockUploadTask.addOnCompleteListener(any())).thenReturn(mockUploadTask)
        
//         Mockito.`when`(mockProfileImagesRef.downloadUrl).thenReturn(mockDownloadUrlTask)
//         Mockito.`when`(mockUriResult.toString()).thenReturn(testDownloadUrl)
        
//         // Act
//         val result = firebaseStorageUtil.uploadProfileImage(mockContext, mockUri, testUserId)
        
//         // Assert
//         assertTrue(result.isSuccess)
//         assertEquals(testDownloadUrl, result.getOrNull())
//     }
    
//     @Test
//     fun testUploadProfileImageFailure() = runTest {
//         // Arrange
//         val mockUploadTask = Mockito.mock(UploadTask::class.java)
//         val exception = Exception("Upload failed")
//         val mockFailedTask: Task<UploadTask.TaskSnapshot> = Tasks.forException(exception)
        
//         Mockito.`when`(mockProfileImagesRef.putFile(mockUri)).thenReturn(mockUploadTask)
//         Mockito.`when`(mockUploadTask.await()).thenAnswer { throw exception }
//         Mockito.`when`(mockUploadTask.addOnSuccessListener(any())).thenReturn(mockUploadTask)
//         Mockito.`when`(mockUploadTask.addOnFailureListener(any())).thenReturn(mockUploadTask)
//         Mockito.`when`(mockUploadTask.addOnCompleteListener(any())).thenReturn(mockUploadTask)
        
//         // Act
//         val result = firebaseStorageUtil.uploadProfileImage(mockContext, mockUri, testUserId)
        
//         // Assert
//         assertTrue(result.isFailure)
//         assertEquals(exception, result.exceptionOrNull())
//     }
    
//     @Test
//     fun testDeleteProfileImageSuccess() = runTest {
//         // Arrange
//         val mockImageRef = Mockito.mock(StorageReference::class.java)
//         val mockDeleteTask: Task<Void> = Tasks.forResult(null)
        
//         Mockito.`when`(mockStorage.getReferenceFromUrl(testDownloadUrl)).thenReturn(mockImageRef)
//         Mockito.`when`(mockImageRef.delete()).thenReturn(mockDeleteTask)
        
//         // Act
//         val result = firebaseStorageUtil.deleteProfileImage(testDownloadUrl)
        
//         // Assert
//         assertTrue(result.isSuccess)
//     }
    
//     @Test
//     fun testDeleteProfileImageFailure() = runTest {
//         // Arrange
//         val mockImageRef = Mockito.mock(StorageReference::class.java)
//         val exception = Exception("Delete failed")
//         val mockDeleteTask: Task<Void> = Tasks.forException(exception)
        
//         Mockito.`when`(mockStorage.getReferenceFromUrl(testDownloadUrl)).thenReturn(mockImageRef)
//         Mockito.`when`(mockImageRef.delete()).thenReturn(mockDeleteTask)
        
//         // Act
//         val result = firebaseStorageUtil.deleteProfileImage(testDownloadUrl)
        
//         // Assert
//         assertTrue(result.isFailure)
//         assertEquals(exception, result.exceptionOrNull())
//     }
    
//     @After
//     fun tearDown() {
//         // Close the static mock to avoid memory leaks
//         if (::mockedMimeTypeMapStatic.isInitialized) {
//             mockedMimeTypeMapStatic.close()
//         }
//     }
// }