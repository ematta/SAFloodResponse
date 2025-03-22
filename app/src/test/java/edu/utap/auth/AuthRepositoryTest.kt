package edu.utap.auth

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import edu.utap.auth.db.UserDao
import edu.utap.auth.repository.AuthRepositoryInterface
import edu.utap.user.UserRepository
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
class AuthRepositoryTest {
    
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userDao: UserDao
    private lateinit var userRepository: UserRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var mockAuthResult: AuthResult
    private lateinit var mockUser: FirebaseUser
    
    @Before
    fun setup() {
        firebaseAuth = mock(FirebaseAuth::class.java)
        userDao = mock(UserDao::class.java)
        userRepository = mock(UserRepository::class.java)
        mockAuthResult = mock(AuthResult::class.java)
        mockUser = mock(FirebaseUser::class.java)
        
        `when`(mockAuthResult.user).thenReturn(mockUser)
        
        authRepository = AuthRepository(firebaseAuth, userDao, userRepository)
    }
    
    @Test
    fun `registerUser returns success when registration succeeds`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val name = "Test User"
        val successTask: Task<AuthResult> = Tasks.forResult(mockAuthResult)
        
        `when`(firebaseAuth.createUserWithEmailAndPassword(email, password)).thenReturn(successTask)
        
        // When
        val result = authRepository.registerUser(email, password, name)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockUser, result.getOrNull())
        verify(firebaseAuth).createUserWithEmailAndPassword(email, password)
    }
    
    @Test
    fun `registerUser returns failure when registration fails`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val name = "Test User"
        val exception = Exception("Registration failed")
        val failureTask: Task<AuthResult> = Tasks.forException(exception)
        
        `when`(firebaseAuth.createUserWithEmailAndPassword(email, password)).thenReturn(failureTask)
        
        // When
        val result = authRepository.registerUser(email, password, name)
        
        // Then
        assertTrue(result.isFailure)
        verify(firebaseAuth).createUserWithEmailAndPassword(email, password)
    }
    
    @Test
    fun `loginUser returns success when login succeeds`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val successTask: Task<AuthResult> = Tasks.forResult(mockAuthResult)
        
        `when`(firebaseAuth.signInWithEmailAndPassword(email, password)).thenReturn(successTask)
        `when`(mockUser.uid).thenReturn("test-uid")
        `when`(mockUser.email).thenReturn(email)
        
        // When
        val result = authRepository.loginUser(email, password)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockUser, result.getOrNull())
        verify(firebaseAuth).signInWithEmailAndPassword(email, password)
    }
    
    @Test
    fun `loginUser returns failure when login fails`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val exception = Exception("Login failed")
        val failureTask: Task<AuthResult> = Tasks.forException(exception)
        
        `when`(firebaseAuth.signInWithEmailAndPassword(email, password)).thenReturn(failureTask)
        
        // When
        val result = authRepository.loginUser(email, password)
        
        // Then
        assertTrue(result.isFailure)
        verify(firebaseAuth).signInWithEmailAndPassword(email, password)
    }
    
    @Test
    fun `getCurrentUser returns current user`() = runTest {
        // Given
        `when`(firebaseAuth.currentUser).thenReturn(mockUser)
        
        // When
        val result = authRepository.getCurrentUser()
        
        // Then
        assertEquals(mockUser, result)
        verify(firebaseAuth).currentUser
    }
    
    @Test
    fun `logout calls signOut`() = runTest {
        // When
        authRepository.logout()
        
        // Then
        verify(firebaseAuth).signOut()
    }
}