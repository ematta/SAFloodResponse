package edu.utap.flood.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import edu.utap.flood.db.FloodReportDao
import edu.utap.flood.db.FloodReportEntity
import edu.utap.flood.model.FloodReport
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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
    private lateinit var dao: FloodReportDao
    private lateinit var collectionRef: CollectionReference
    private lateinit var documentRef: DocumentReference

    @Before
    fun setup() {
        firestore = mockk(relaxed = true)
        dao = mockk(relaxed = true)
        collectionRef = mockk(relaxed = true)
        documentRef = mockk(relaxed = true)

        every { firestore.collection(any()) } returns collectionRef
        every { collectionRef.document(any()) } returns documentRef

        repository = FloodReportRepository(firestore, dao)
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

    private fun createTestEntity(): FloodReportEntity {
        val now = Date()
        return FloodReportEntity(
            reportId = "test_report_id",
            userId = "test_user_id",
            latitude = 30.2672,
            longitude = -97.7431,
            description = "Test flood report",
            photoUrls = listOf("test_photo.jpg"),
            status = "pending",
            createdAt = now,
            updatedAt = now,
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
        coVerify { dao.insertReport(any()) }
        verify { documentRef.set(any()) }
    }

    @Test
    fun testCreateReportFailure() = runTest {
        val report = createTestReport()
        val task: Task<Void> = Tasks.forException(Exception("Failed to create report"))
        every { documentRef.set(any()) } returns task

        val result = repository.createReport(report)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { dao.insertReport(any()) }
    }

    @Test
    fun testGetReportByIdLocalSuccess() = runTest {
        val testReportId = "test_report_id"
        val entity = createTestEntity()
        coEvery { dao.getReportById(testReportId) } returns entity

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
        coVerify { dao.insertReport(any()) }
    }

    @Test
    fun testGetReportByIdNotFound() = runTest {
        val testReportId = "nonexistent_id"
        coEvery { dao.getReportById(testReportId) } returns null
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
        coVerify { dao.updateReport(any()) }
        verify { documentRef.set(any()) }
    }

    @Test
    fun testDeleteReportSuccess() = runTest {
        val testReportId = "test_report_id"
        val task: Task<Void> = Tasks.forResult(null)
        every { documentRef.delete() } returns task

        val result = repository.deleteReport(testReportId)

        assertTrue(result.isSuccess)
        coVerify { dao.deleteReport(any()) }
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