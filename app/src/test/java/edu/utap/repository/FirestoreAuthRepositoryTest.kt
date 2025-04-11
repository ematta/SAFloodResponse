// package edu.utap.repository

// import com.google.android.gms.tasks.Task
// import com.google.firebase.auth.AuthResult
// import com.google.firebase.auth.FirebaseAuth
// import com.google.firebase.auth.FirebaseUser
// import com.google.firebase.auth.UserProfileChangeRequest
// import com.google.firebase.firestore.CollectionReference
// import com.google.firebase.firestore.DocumentReference
// import com.google.firebase.firestore.FirebaseFirestore
// import edu.utap.auth.MainDispatcherRule
// import io.mockk.MockKAnnotations
// import io.mockk.coEvery
// import io.mockk.coVerify
// import io.mockk.every
// import io.mockk.impl.annotations.MockK
// import io.mockk.mockk
// import kotlinx.coroutines.ExperimentalCoroutinesApi
// import kotlinx.coroutines.tasks.await
// import kotlinx.coroutines.test.runTest
// import org.junit.Assert
// import org.junit.Before
// import org.junit.Rule
// import org.junit.Test
// import kotlin.Exception
// import kotlin.Result
// import kotlin.test.assertTrue

// @ExperimentalCoroutinesApi
// class FirestoreAuthRepositoryTest {

//     @get:Rule
//     val mainDispatcherRule = MainDispatcherRule()

//     @MockK
//     private lateinit var firebaseAuth: FirebaseAuth

//     @MockK
//     private lateinit var firestore: FirebaseFirestore

//     @MockK
//     private lateinit var mockAuthResult: AuthResult

//     @MockK
//     private lateinit var mockUser: FirebaseUser

//     @MockK
//     private lateinit var usersCollection: CollectionReference

//     @MockK
//     private lateinit var userDocument: DocumentReference

//     private lateinit var repository: FirestoreAuthRepository

//     @Before
//     fun setup() {
//         MockKAnnotations.init(this)

//         every { firestore.collection("users") } returns usersCollection
//         every { usersCollection.document(any()) } returns userDocument

//         repository = FirestoreAuthRepository(firebaseAuth, firestore)
//     }

//     @Test
//     fun registerUser_success() = runTest {
//         val email = "test@user.com"
//         val password = "password"
//         val name = "Test User"
//         val role = "regular"

//         val authResult = mockk<AuthResult>()
//         val user = mockk<FirebaseUser>()
//         val profileUpdateRequest = mockk<UserProfileChangeRequest>()
//         val voidTask = mockk<Void>()

//         every { authResult.user } returns user
//         coEvery { firebaseAuth.createUserWithEmailAndPassword(email, password).await() } returns authResult
//         every { user.uid } returns "uid123"
//         coEvery { user.updateProfile(any()).await() } returns voidTask
//         coEvery { user.updateProfile(any()).await() } returns voidTask
//         coEvery { userDocument.set(any()).await() } returns voidTask

//         val result = repository.registerUser(email, password, name, role)

//         assertTrue(result.isSuccess)
//         Assert.assertEquals(user, result.getOrNull())
//     }

//     @Test
//     fun registerUser_failure() = runTest {
//         val email = "test@user.com"
//         val password = "password"
//         val name = "Test User"
//         val role = "regular"

//         val exception = Exception("Registration failed")
//         coEvery { firebaseAuth.createUserWithEmailAndPassword(email, password).await() } throws exception

//         val result = repository.registerUser(email, password, name, role)

//         assertTrue(result.isFailure)
//         Assert.assertEquals(exception.message, result.exceptionOrNull()?.message)
//     }

//     @Test
//     fun loginUser_success() = runTest {
//         val email = "test@user.com"
//         val password = "password"

//         val authResult = mockk<AuthResult>()
//         val user = mockk<FirebaseUser>()
//         val voidTask = mockk<Void>()

//         every { authResult.user } returns user
//         coEvery { firebaseAuth.signInWithEmailAndPassword(email, password).await() } returns authResult
//         every { user.uid } returns "uid123"
//         coEvery { userDocument.set(any()).await() } returns voidTask

//         val result = repository.loginUser(email, password)

//         assertTrue(result.isSuccess)
//         Assert.assertEquals(user, result.getOrNull())
//     }

//     @Test
//     fun loginUser_failure() = runTest {
//         val email = "test@user.com"
//         val password = "password"

//         val exception = Exception("Login failed")
//         coEvery { firebaseAuth.signInWithEmailAndPassword(email, password).await() } throws exception

//         val result = repository.loginUser(email, password)

//         assertTrue(result.isFailure)
//         Assert.assertEquals(exception.message, result.exceptionOrNull()?.message)
//     }

//     @Test
//     fun resetPassword_success() = runTest {
//         val email = "test@user.com"
//         val voidTask = mockk<Void>()

//         coEvery { firebaseAuth.sendPasswordResetEmail(any()).await() } returns voidTask

//         val result = repository.resetPassword(email)

//         assertTrue(result.isSuccess)
//     }

//     @Test
//     fun resetPassword_failure() = runTest {
//         val email = "test@user.com"
//         val exception = Exception("Reset failed")

//         coEvery { firebaseAuth.sendPasswordResetEmail(email).await() } throws exception

//         val result = repository.resetPassword(email)

//         assertTrue(result.isFailure)
//         Assert.assertEquals(exception.message, result.exceptionOrNull()?.message)
//     }

//     @Test
//     fun getCurrentUser_returnsUser() = runTest {
//         every { firebaseAuth.currentUser } returns mockUser

//         val result = repository.getCurrentUser()

//         Assert.assertEquals(mockUser, result)
//     }

//     @Test
//     fun logout_callsSignOut() = runTest {
//         every { firebaseAuth.signOut() } returns Unit

//         repository.logout()

//         io.mockk.verify { firebaseAuth.signOut() }
//     }
// }
