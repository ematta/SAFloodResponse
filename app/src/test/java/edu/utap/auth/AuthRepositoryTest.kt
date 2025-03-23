package edu.utap.auth

import android.net.Uri
import android.text.TextUtils
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import edu.utap.auth.db.UserDao
import edu.utap.auth.db.UserEntity
import edu.utap.user.MainDispatcherRule
import edu.utap.user.UserProfile
import edu.utap.user.UserRepository
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.net.URI

@ExperimentalCoroutinesApi
class AuthRepositoryTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    @Mock
    private lateinit var firebaseAuth: FirebaseAuth
    
    @Mock
    private lateinit var userDao: UserDao
    
    @Mock
    private lateinit var userRepository: UserRepository
    
    @Mock
    private lateinit var mockAuthResult: AuthResult
    
    @Mock
    private lateinit var mockUser: FirebaseUser
    
    private lateinit var authRepository: AuthRepository
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Setup mock behaviors
        Mockito.`when`(mockAuthResult.user).thenReturn(mockUser)
        Mockito.`when`(mockUser.uid).thenReturn("test-uid")
        Mockito.`when`(mockUser.updateProfile(any())).thenReturn(Tasks.forResult(null))
        
        // Setup repository
        authRepository = AuthRepository(firebaseAuth, userDao, userRepository)
    }
    
    @Test
    fun testRegisterUserSuccess() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val name = "Test User"
        val successTask: Task<AuthResult> = Tasks.forResult(mockAuthResult)
        
        // Setup mock behaviors
        Mockito.`when`(firebaseAuth.createUserWithEmailAndPassword(eq(email), eq(password)))
            .thenReturn(successTask)
        
        // Set up mock for userRepository.createUserProfile
        Mockito.`when`(userRepository.createUserProfile(any()))
            .thenReturn(Result.success(UserProfile(uid = "test-uid", displayName = name, email = email)))
            
        // Set up mock for userDao operations
        Mockito.`when`(userDao.insertUser(any()))
            .thenReturn(Unit)
            
        // Set up mock for email and displayName
        Mockito.`when`(mockUser.email).thenReturn(email)
        Mockito.`when`(mockUser.displayName).thenReturn(name)

        // Set up mock for android.text.TextUtils.isEmpty
        val mockedTextUtils = mockStatic(TextUtils::class.java)
        mockedTextUtils.`when`<Boolean> { TextUtils.isEmpty(any()) }.thenReturn(false)

        // Set up mock for Uri.parse()
        val mockedUri = mockStatic(Uri::class.java)
        mockedUri.`when`<Uri> { Uri.parse(any()) }.thenReturn(Uri.EMPTY)

        try {
            // When
            val result = authRepository.registerUser(email, password, name)
            
            // Then
            assertTrue("Result should be success, but was ${result.exceptionOrNull()}", result.isSuccess)
            assertEquals(mockUser, result.getOrNull())
            Mockito.verify(firebaseAuth).createUserWithEmailAndPassword(eq(email), eq(password))
            Mockito.verify(userDao).insertUser(any())
            Mockito.verify(userRepository).createUserProfile(any())
        } finally {
            mockedTextUtils.close()
            mockedUri.close()
        }
    }

    @Test
    fun testRegisterUserFailure() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val name = "Test User"
        val exception = Exception("Registration failed")
        val failureTask: Task<AuthResult> = Tasks.forException(exception)
        
        Mockito.`when`(firebaseAuth.createUserWithEmailAndPassword(eq(email), eq(password)))
            .thenReturn(failureTask)
        
        // When
        val result = authRepository.registerUser(email, password, name)
        
        // Then
        assertTrue(result.isFailure)
        Mockito.verify(firebaseAuth).createUserWithEmailAndPassword(eq(email), eq(password))
    }
    
    @Test
    fun testLoginUserSuccess() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val successTask: Task<AuthResult> = Tasks.forResult(mockAuthResult)
        val userEntity = UserEntity(
            userId = "test-uid",
            name = "Test User",
            email = email,
            profilePic = null,
            role = "regular"
        )
        
        // Setup mock behaviors
        Mockito.`when`(firebaseAuth.signInWithEmailAndPassword(any(), any()))
            .thenReturn(successTask)
        Mockito.`when`(mockUser.email).thenReturn(email)
        
        // Mock userRepository.getUserProfile to return success
        Mockito.`when`(userRepository.getUserProfile(any()))
            .thenReturn(Result.success(UserProfile(uid = "test-uid", displayName = "Test User", email = email)))
        
        // Mock userDao.getUserById to return null (user doesn't exist locally)
        Mockito.`when`(userDao.getUserById(any()))
            .thenReturn(null)
            
        // Mock syncUserToLocal to return success
        Mockito.`when`(userDao.insertUser(any()))
            .thenReturn(Unit)
        
        // When
        val result = authRepository.loginUser(email, password)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockUser, result.getOrNull())
        Mockito.verify(firebaseAuth).signInWithEmailAndPassword(eq(email), eq(password))
    }
    
    @Test
    fun testLoginUserFailure() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val exception = Exception("Login failed")
        val failureTask: Task<AuthResult> = Tasks.forException(exception)
        
        // Set up mock for android.text.TextUtils.isEmpty
        val mockedTextUtils = mockStatic(TextUtils::class.java)
        mockedTextUtils.`when`<Boolean> { TextUtils.isEmpty(any()) }.thenReturn(false)

        // Set up mock for Uri.parse()
        val mockedUri = mockStatic(Uri::class.java)
        mockedUri.`when`<Uri> { Uri.parse(any()) }.thenReturn(Uri.EMPTY)
        
        try {
            Mockito.`when`(firebaseAuth.signInWithEmailAndPassword(eq(email), eq(password)))
                .thenReturn(failureTask)
            
            // When
            val result = authRepository.loginUser(email, password)
            
            // Then
            assertTrue(result.isFailure)
            Mockito.verify(firebaseAuth).signInWithEmailAndPassword(eq(email), eq(password))
        } finally {
            mockedTextUtils.close()
            mockedUri.close()
        }
    }
    
    @Test
    fun testGetCurrentUser() = runTest {
        // Given
        Mockito.`when`(firebaseAuth.currentUser).thenReturn(mockUser)
        
        // When
        val result = authRepository.getCurrentUser()
        
        // Then
        assertEquals(mockUser, result)
        Mockito.verify(firebaseAuth).currentUser
    }
    
    @Test
    fun testLogout() = runTest {
        // When
        authRepository.logout()
        
        // Then
        Mockito.verify(firebaseAuth).signOut()
    }
    
    @Test
    fun testResetPasswordSuccess() = runTest {
        // Given
        val email = "test@example.com"
        val successTask: Task<Void> = Tasks.forResult(null)
        
        // Setup mock behavior
        Mockito.`when`(firebaseAuth.sendPasswordResetEmail(eq(email)))
            .thenReturn(successTask)
        
        // When
        val result = authRepository.resetPassword(email)
        
        // Then
        assertTrue("Result should be success, but was ${result.exceptionOrNull()}", result.isSuccess)
        Mockito.verify(firebaseAuth).sendPasswordResetEmail(eq(email))
    }
    
    @Test
    fun testResetPasswordFailure() = runTest {
        // Given
        val email = "test@example.com"
        val exception = Exception("Password reset failed")
        val failureTask: Task<Void> = Tasks.forException(exception)
        
        // Setup mock behavior
        Mockito.`when`(firebaseAuth.sendPasswordResetEmail(eq(email)))
            .thenReturn(failureTask)
        
        // When
        val result = authRepository.resetPassword(email)
        
        // Then
        assertTrue("Result should be failure", result.isFailure)
        assertEquals(exception.message, result.exceptionOrNull()?.message)
        Mockito.verify(firebaseAuth).sendPasswordResetEmail(eq(email))
    }
}