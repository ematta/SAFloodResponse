package edu.utap.flood.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import edu.utap.flood.model.FloodReport
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class FloodReportRepositoryTest {
    @get:Rule
    val testDispatcher = TestDispatcherRule()

    private lateinit var repository: FloodReportRepository
    private lateinit var firestore: FirebaseFirestore
    private lateinit var collectionRef: CollectionReference
    private lateinit var documentRef: DocumentReference

    @Before
    fun setup() {
        firestore = mockk(relaxed = true)
        collectionRef = mockk(relaxed = true)
        documentRef = mockk(relaxed = true)

        every { firestore.collection(any()) } returns collectionRef
        every { collectionRef.document(any()) } returns documentRef

        repository = FloodReportRepository(firestore)
    }

    private fun createTestReport(): FloodReport {
        return FloodReport(
            reportId = "test_report_id",
            userId = "test_user_id",
            latitude = 30.2672,
            longitude = -97.7431,
            description = "Test flood report",
            photoUrls = listOf("test_photo.jpg"),
            status = "pending",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now(),
            isManualLocation = false,
            confirmedCount = 0,
            deniedCount = 0
        )
    }

    @Test
    fun testCreateReportSuccess() = runTest {
        val report = createTestReport()
        val task: Task<Void> = Tasks.forResult(null)
        every { documentRef.set(any()) } returns task

        val result = repository.createReport(report)

        assertTrue(result.isSuccess)
        verify { documentRef.set(any()) }
    }

    @Test
    fun testCreateReportFailure() = runTest {
        val report = createTestReport()
        val task: Task<Void> = Tasks.forException(Exception("Failed to create report"))
        every { documentRef.set(any()) } returns task

        val result = repository.createReport(report)

        assertTrue(result.isFailure)
    }

    @Test
    fun testGetReportByIdLocalSuccess() = runTest {
        val testReportId = "test_report_id"

        val result = repository.getReportById(testReportId)

        assertTrue(result.isSuccess)
        assertEquals(testReportId, result.getOrNull()?.reportId)
    }

    @Test
    fun testGetReportByIdRemoteSuccess() = runTest {
        val testReportId = "test_report_id"
        val documentSnapshot: DocumentSnapshot = mockk(relaxed = true)
        val task: Task<DocumentSnapshot> = Tasks.forResult(documentSnapshot)
        every { documentRef.get() } returns task
        every { documentSnapshot.exists() } returns true
        every { documentSnapshot.toObject(FloodReport::class.java) } returns createTestReport()

        val result = repository.getReportById(testReportId)

        assertTrue(result.isSuccess)
        assertEquals(testReportId, result.getOrNull()?.reportId)
    }

    @Test
    fun testGetReportByIdNotFound() = runTest {
        val testReportId = "nonexistent_id"
        val documentSnapshot: DocumentSnapshot = mockk(relaxed = true)
        val task: Task<DocumentSnapshot> = Tasks.forResult(documentSnapshot)
        every { documentRef.get() } returns task
        every { documentSnapshot.exists() } returns false

        val result = repository.getReportById(testReportId)

        assertTrue(result.isFailure)
        assertEquals("Report not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun testUpdateReportSuccess() = runTest {
        val report = createTestReport()
        val task: Task<Void> = Tasks.forResult(null)
        every { documentRef.set(any()) } returns task

        val result = repository.updateReport(report)

        assertTrue(result.isSuccess)
        verify { documentRef.set(any()) }
    }

    @Test
    fun testDeleteReportSuccess() = runTest {
        val testReportId = "test_report_id"
        val task: Task<Void> = Tasks.forResult(null)
        every { documentRef.delete() } returns task

        val result = repository.deleteReport(testReportId)

        assertTrue(result.isSuccess)
        verify { documentRef.delete() }
    }
}

class TestDispatcherRule : TestRule {
    private val testDispatcher = StandardTestDispatcher()
    override fun apply(base: org.junit.runners.model.Statement, description: org.junit.runner.Description) =
        object : org.junit.runners.model.Statement() {
            override fun evaluate() {
                kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
                try {
                    base.evaluate()
                } finally {
                    kotlinx.coroutines.Dispatchers.resetMain()
                }
            }
        }
}
