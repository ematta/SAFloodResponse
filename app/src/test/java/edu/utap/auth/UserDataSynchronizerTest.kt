package edu.utap.auth

import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import edu.utap.repository.AuthRepositoryInterface
import edu.utap.repository.UserRepository
import edu.utap.models.UserProfile
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import edu.utap.user.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class UserDataSynchronizerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    lateinit var authRepository: AuthRepositoryInterface

    @MockK
    lateinit var userRepository: UserRepository

    @MockK
    lateinit var firebaseUser: FirebaseUser

    private lateinit var synchronizer: UserDataSynchronizer

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        synchronizer = UserDataSynchronizer(authRepository, userRepository)
        firebaseUser = mockk()
    }

    @Test
    fun `syncUserToLocal returns success and updates user profile`() = runTest {
        val user = edu.utap.utils.Result.Success(UserProfile())
        coEvery { authRepository.getCurrentUser()} returns firebaseUser
        coEvery { userRepository.updateUserProfile(any()) } returns user
        coEvery { firebaseUser.getUid() } returns "123"
        coEvery { firebaseUser.getDisplayName() } returns "123"
        coEvery { firebaseUser.getEmail() } returns "123"
        coEvery { firebaseUser.getPhotoUrl() } returns Uri.parse("123")
        coEvery { firebaseUser.getPhoneNumber() } returns "123"

        val result = synchronizer.syncUserToLocal(firebaseUser)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `syncUserToLocal returns failure when updateUserProfile throws`() = runTest {
        coEvery { userRepository.updateUserProfile(any()) } throws RuntimeException("Update failed")

        val result = synchronizer.syncUserToLocal(firebaseUser)

        assertTrue(result.isFailure)
    }
}
