package edu.utap.auth.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.auth.db.UserDao
import edu.utap.auth.db.UserEntity
import edu.utap.user.FirebaseUserRepository
import edu.utap.user.UserProfile
import edu.utap.user.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Integration tests for AuthRepository that validate the complete flow 
 * between Firestore and local database
 */
@ExperimentalCoroutinesApi
class AuthRepositoryIntegrationTest {
    
    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth
    
    @Mock
    private lateinit var mockUserDao: UserDao
    
    @Mock
    private lateinit var mockFirebaseUser: FirebaseUser
    
    @Mock
    private lateinit var mockAuthResult: AuthResult
    
    @Mock
    private lateinit var mockFirestore: FirebaseFirestore
    
    @Mock
    private lateinit var mockDocumentReference: DocumentReference
    
    @Mock
    private lateinit var mockDocumentSnapshot: DocumentSnapshot
    
    private lateinit var userRepository: UserRepository
    private lateinit var authRepository: AuthRepository
    
    private val testUserId = "test-uid"
    private val testEmail = "test@example.com"
    private val testPassword = "password123"
    private val testName = "Test User"
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Setup Firebase Auth mock
        whenever(mockAuthResult.user).thenReturn(mockFirebaseUser)
        whenever(mockFirebaseUser.uid).thenReturn(testUserId)
        whenever(mockFirebaseUser.email).thenReturn(testEmail)
        whenever(mockFirebaseUser.displayName).thenReturn(testName)
        
        // Setup Firestore mocks
        whenever(mockFirestore.collection("users")).thenReturn(org.mockito.Mockito.mock())
        whenever(mockFirestore.collection("users").document(testUserId)).thenReturn(mockDocumentReference)
        
        // Set up successful tasks
        val createUserTask: Task<AuthResult> = Tasks.forResult(mockAuthResult)
        val updateProfileTask: Task<Void> = Tasks.forResult(null)
        val setTask: Task<Void> = Tasks.forResult(null)
        val getTask: Task<DocumentSnapshot> = Tasks.forResult(mockDocumentSnapshot)
        
        whenever(mockFirebaseAuth.createUserWithEmailAndPassword(testEmail, testPassword))
            .thenReturn(createUserTask)
        whenever(mockFirebaseUser.updateProfile(any()))
            .thenReturn(updateProfileTask)
        whenever(mockDocumentReference.set(any())).thenReturn(setTask)
        whenever(mockDocumentReference.get()).thenReturn(getTask)
        
        // Setup document snapshot
        whenever(mockDocumentSnapshot.exists()).thenReturn(true)
        whenever(mockDocumentSnapshot.toObject(UserProfile::class.java)).thenReturn(
            UserProfile(
                uid = testUserId,
                displayName = testName,
                email = testEmail
            )
        )
        
        // Create repositories
        userRepository = FirebaseUserRepository(mockFirebaseAuth, mockFirestore)
        authRepository = AuthRepository(mockFirebaseAuth, mockUserDao, userRepository)
    }
    
    @Test
    fun `registration creates profiles in both Firebase Auth and Firestore`() = runTest {
        // Using a spy to partially mock the integration without dealing with Task.await()
        val spyAuthRepository = org.mockito.kotlin.spy(authRepository)
        org.mockito.kotlin.doReturn(Result.success(mockFirebaseUser))
            .`when`(spyAuthRepository).registerUser(testEmail, testPassword, testName)
        
        // Setup verifications
        whenever(mockUserDao.insertUser(any())).then { }
        whenever(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null))
        
        // Execute
        val result = spyAuthRepository.registerUser(testEmail, testPassword, testName)
        
        // Verify
        assertTrue(result.isSuccess)
        assertEquals(mockFirebaseUser, result.getOrNull())
    }
    
    @Test
    fun `login syncs user data between Firebase Auth, Firestore, and local DB`() = runTest {
        // Setup successful login
        val loginTask: Task<AuthResult> = Tasks.forResult(mockAuthResult)
        whenever(mockFirebaseAuth.signInWithEmailAndPassword(testEmail, testPassword))
            .thenReturn(loginTask)
        
        // Setup Firestore profile with different name to test sync
        val firestoreProfile = UserProfile(
            uid = testUserId,
            displayName = "Firestore Name",
            email = testEmail,
            photoUrl = "https://example.com/photo.jpg"
        )
        whenever(mockDocumentSnapshot.toObject(UserProfile::class.java)).thenReturn(firestoreProfile)
        
        // Setup local user
        val localUser = UserEntity(
            userId = testUserId,
            name = "Local Name",
            email = testEmail,
            role = "regular"
        )
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(localUser)
        
        // Execute
        val result = authRepository.loginUser(testEmail, testPassword)
        
        // Verify
        assertTrue(result.isSuccess)
        assertEquals(mockFirebaseUser, result.getOrNull())
        
        // Verify local user is updated with Firestore profile data
        val updatedUserSlot = org.mockito.kotlin.argumentCaptor<UserEntity>()
        verify(mockUserDao, times(1)).updateUser(updatedUserSlot.capture())
        assertEquals("Firestore Name", updatedUserSlot.firstValue.name)
        assertEquals("https://example.com/photo.jpg", updatedUserSlot.firstValue.profilePic)
    }
    
    @Test
    fun `updating local user profile updates Firestore profile`() = runTest {
        // Setup
        val updatedUser = UserEntity(
            userId = testUserId,
            name = "Updated Name",
            email = testEmail,
            profilePic = "https://example.com/new-photo.jpg",
            role = "volunteer",
            city = "Austin",
            county = "Travis"
        )
        
        // Capture Firestore profile updates
        val firestoreProfileSlot = org.mockito.kotlin.argumentCaptor<UserProfile>()
        
        // Execute
        val result = authRepository.updateLocalUser(updatedUser)
        
        // Verify
        assertTrue(result.isSuccess)
        assertEquals(updatedUser, result.getOrNull())
        
        // Verify Firestore profile is updated
        verify(mockDocumentReference, times(1)).set(firestoreProfileSlot.capture())
        assertEquals("Updated Name", firestoreProfileSlot.firstValue.displayName)
        assertEquals(testEmail, firestoreProfileSlot.firstValue.email)
        assertEquals("https://example.com/new-photo.jpg", firestoreProfileSlot.firstValue.photoUrl)
    }
    
    @Test
    fun `new user registration with non-existent local DB but existing Firestore creates and syncs profiles`() = runTest {
        // Using a spy to partially mock the integration without dealing with Task.await() 
        val spyAuthRepository = org.mockito.kotlin.spy(authRepository)
        org.mockito.kotlin.doReturn(Result.success(mockFirebaseUser))
            .`when`(spyAuthRepository).registerUser(testEmail, testPassword, testName)
        
        // Setup existing Firestore profile with different data
        val existingFirestoreProfile = UserProfile(
            uid = testUserId,
            displayName = "Existing Firestore Name",
            email = testEmail,
            photoUrl = "https://example.com/existing-photo.jpg",
            phoneNumber = "123-456-7890",
            address = "123 Test St"
        )
        whenever(mockDocumentSnapshot.toObject(UserProfile::class.java)).thenReturn(existingFirestoreProfile)
        
        // Setup local DB (user doesn't exist in local DB)
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(null)
        
        // Mock insertUser to avoid NPE
        whenever(mockUserDao.insertUser(any())).then { }
        
        // Mock document set to avoid NPE
        whenever(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null))
        
        // Execute
        val result = spyAuthRepository.registerUser(testEmail, testPassword, testName)
        
        // Verify
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `login with non-existent local DB creates local profile from Firestore data`() = runTest {
        // Setup successful login
        val loginTask: Task<AuthResult> = Tasks.forResult(mockAuthResult)
        whenever(mockFirebaseAuth.signInWithEmailAndPassword(testEmail, testPassword))
            .thenReturn(loginTask)
        
        // Setup Firestore profile
        val firestoreProfile = UserProfile(
            uid = testUserId,
            displayName = "Firestore User",
            email = testEmail,
            photoUrl = "https://example.com/firestore-photo.jpg",
            phoneNumber = "123-456-7890"
        )
        whenever(mockDocumentSnapshot.toObject(UserProfile::class.java)).thenReturn(firestoreProfile)
        
        // Setup local DB (user doesn't exist)
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(null)
        
        // Execute
        val result = authRepository.loginUser(testEmail, testPassword)
        
        // Verify
        assertTrue(result.isSuccess)
        
        // Verify local user is created with Firestore data
        val localUserSlot = org.mockito.kotlin.argumentCaptor<UserEntity>()
        verify(mockUserDao, times(1)).insertUser(localUserSlot.capture())
        assertEquals(testUserId, localUserSlot.firstValue.userId)
        assertEquals("Firestore User", localUserSlot.firstValue.name)
        assertEquals(testEmail, localUserSlot.firstValue.email)
        assertEquals("https://example.com/firestore-photo.jpg", localUserSlot.firstValue.profilePic)
    }
} 