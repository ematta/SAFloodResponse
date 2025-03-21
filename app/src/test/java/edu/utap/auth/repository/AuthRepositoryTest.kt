package edu.utap.auth.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import edu.utap.auth.db.UserDao
import edu.utap.auth.db.UserEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import kotlinx.coroutines.tasks.await
import org.mockito.kotlin.doAnswer

@ExperimentalCoroutinesApi
class AuthRepositoryTest {
    
    private lateinit var mockFirebaseAuth: FirebaseAuth
    private lateinit var mockUserDao: UserDao
    private lateinit var mockFirebaseUser: FirebaseUser
    private lateinit var authRepository: AuthRepository
    
    private val testUserId = "test-uid"
    private val testEmail = "test@example.com"
    private val testPassword = "password123"
    private val testName = "Test User"
    
    @Before
    fun setup() {
        // Setup mocks
        mockFirebaseAuth = mock(FirebaseAuth::class.java)
        mockUserDao = mock(UserDao::class.java)
        mockFirebaseUser = mock(FirebaseUser::class.java)
        
        // Setup user data
        whenever(mockFirebaseUser.uid).thenReturn(testUserId)
        whenever(mockFirebaseUser.email).thenReturn(testEmail)
        whenever(mockFirebaseUser.displayName).thenReturn(testName)
        
        // Setup authentication
        authRepository = AuthRepository(mockFirebaseAuth, mockUserDao)
    }
    
    @Test
    fun `isAuthenticated returns true when user is logged in`() = runTest {
        whenever(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        
        val result = authRepository.isAuthenticated()
        
        assertTrue(result)
    }
    
    @Test
    fun `isAuthenticated returns false when user is not logged in`() = runTest {
        whenever(mockFirebaseAuth.currentUser).thenReturn(null)
        
        val result = authRepository.isAuthenticated()
        
        assertFalse(result)
    }
    
    @Test
    fun `getCurrentUser returns current user when authenticated`() = runTest {
        whenever(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        
        val result = authRepository.getCurrentUser()
        
        assertEquals(mockFirebaseUser, result)
    }
    
    @Test
    fun `logout signs out the user`() = runTest {
        authRepository.logout()
        
        verify(mockFirebaseAuth, times(1)).signOut()
    }
    
    @Test
    fun `getLocalUserById returns user when found`() = runTest {
        val testUser = UserEntity(
            userId = testUserId,
            name = testName,
            email = testEmail,
            role = "regular"
        )
        
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(testUser)
        
        val result = authRepository.getLocalUserById(testUserId)
        
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
    }
    
    @Test
    fun `getLocalUserById returns failure when user not found`() = runTest {
        whenever(mockUserDao.getUserById(testUserId)).thenReturn(null)
        
        val result = authRepository.getLocalUserById(testUserId)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("User not found") == true)
    }
    
    @Test
    fun `getLocalUserByEmail returns user when found`() = runTest {
        val testUser = UserEntity(
            userId = testUserId,
            name = testName,
            email = testEmail,
            role = "regular"
        )
        
        whenever(mockUserDao.getUserByEmail(testEmail)).thenReturn(testUser)
        
        val result = authRepository.getLocalUserByEmail(testEmail)
        
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
    }
    
    @Test
    fun `createLocalUser inserts user into database`() = runTest {
        val testUser = UserEntity(
            userId = testUserId,
            name = testName,
            email = testEmail,
            role = "regular"
        )
        
        val result = authRepository.createLocalUser(testUser)
        
        verify(mockUserDao, times(1)).insertUser(testUser)
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
    }
    
    @Test
    fun `updateLocalUser updates user in database`() = runTest {
        val testUser = UserEntity(
            userId = testUserId,
            name = testName,
            email = testEmail,
            role = "regular"
        )
        
        val result = authRepository.updateLocalUser(testUser)
        
        verify(mockUserDao, times(1)).updateUser(testUser)
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
    }
    
    @Test
    fun `observeLocalUsers returns flow of users`() = runTest {
        val testUsers = listOf(
            UserEntity(
                userId = testUserId,
                name = testName,
                email = testEmail,
                role = "regular"
            )
        )
        
        whenever(mockUserDao.getAllUsers()).thenReturn(flowOf(testUsers))
        
        val result = authRepository.observeLocalUsers()
        
        // We can't directly compare Flow objects with assertEquals
        // Instead, collect and compare the values from the flows
        verify(mockUserDao, times(1)).getAllUsers()
        assertEquals(testUsers, result.first())
    }
}