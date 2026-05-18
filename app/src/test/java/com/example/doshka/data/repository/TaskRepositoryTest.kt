package com.example.doshka.data.repository

import app.cash.turbine.test
import com.example.doshka.data.local.dao.LabelDao
import com.example.doshka.data.local.dao.TaskDao
import com.example.doshka.data.local.entity.TaskEntity
import com.example.doshka.data.network.NetworkMonitor
import com.example.doshka.data.remote.api.DoshkaApi
import com.example.doshka.data.remote.dto.TaskDto
import com.example.doshka.data.sync.SyncManager
import com.example.doshka.domain.model.TaskPriority
import com.example.doshka.util.NetworkResult
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.Instant

class TaskRepositoryTest {

    @MockK
    private lateinit var taskDao: TaskDao

    @MockK
    private lateinit var labelDao: LabelDao

    @MockK
    private lateinit var api: DoshkaApi

    @MockK
    private lateinit var syncManager: SyncManager

    @MockK
    private lateinit var networkMonitor: NetworkMonitor

    private lateinit var json: Json
    private lateinit var repository: TaskRepositoryImpl

    private val testTaskEntity = TaskEntity(
        id = "task-1",
        title = "Test Task",
        description = "Description",
        columnId = "column-1",
        position = 0,
        priority = "medium",
        assigneeId = "user-1",
        creatorId = "user-1",
        deadline = null,
        estimatedHours = null,
        actualHours = null,
        tags = "[]",
        attachmentsCount = 0,
        commentsCount = 0,
        isDeleted = false,
        completedAt = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        version = 0,
        isSynced = true
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        every { networkMonitor.isOnline } returns MutableStateFlow(true)

        repository = TaskRepositoryImpl(
            taskDao = taskDao,
            labelDao = labelDao,
            api = api,
            syncManager = syncManager,
            networkMonitor = networkMonitor,
            json = json
        )
    }

    @Test
    fun `observeTasksByColumn returns tasks from dao`() = runTest {
        val tasks = listOf(testTaskEntity)
        every { taskDao.observeTasksByColumn("column-1") } returns flowOf(tasks)

        repository.observeTasksByColumn("column-1").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Test Task", result[0].title)
            cancelAndIgnoreRemainingEvents()
        }

        verify { taskDao.observeTasksByColumn("column-1") }
    }

    @Test
    fun `observeTasksByAssignee returns tasks from dao`() = runTest {
        val tasks = listOf(testTaskEntity)
        every { taskDao.observeTasksByAssignee("user-1") } returns flowOf(tasks)

        repository.observeTasksByAssignee("user-1").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("user-1", result[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTaskById returns task when exists`() = runTest {
        coEvery { taskDao.getTaskById("task-1") } returns testTaskEntity

        val result = repository.getTaskById("task-1")

        assertNotNull(result)
        assertEquals("Test Task", result?.title)
    }

    @Test
    fun `getTaskById returns null when not exists`() = runTest {
        coEvery { taskDao.getTaskById("nonexistent") } returns null

        val result = repository.getTaskById("nonexistent")

        assertNull(result)
    }

    @Test
    fun `createTask saves locally first then syncs`() = runTest {
        coEvery { taskDao.getTasksByColumn(any()) } returns emptyList()
        coEvery { taskDao.insertTask(any()) } just Runs
        coEvery { labelDao.addLabelToTask(any()) } just Runs
        coEvery { syncManager.queueOperation(any(), any(), any(), any<Any>()) } just Runs

        val result = repository.createTask(
            title = "New Task",
            description = "Description",
            columnId = "column-1",
            priority = TaskPriority.HIGH,
            assigneeId = null,
            deadline = null,
            estimatedHours = null,
            tags = emptyList(),
            labelIds = emptyList()
        )

        assertTrue(result is NetworkResult.Success)
        coVerify { taskDao.insertTask(any()) }
    }

    @Test
    fun `deleteTask performs soft delete`() = runTest {
        coEvery { taskDao.softDeleteTask(any()) } just Runs
        coEvery { syncManager.queueOperation(any(), any(), any(), any<Any>()) } just Runs

        val result = repository.deleteTask("task-1")

        assertTrue(result is NetworkResult.Success)
        coVerify { taskDao.softDeleteTask("task-1") }
    }

    @Test
    fun `moveTask updates local db and syncs`() = runTest {
        coEvery { taskDao.moveTask(any(), any(), any()) } just Runs
        coEvery { taskDao.markAsUnsynced(any()) } just Runs
        coEvery { taskDao.getTaskById(any()) } returns testTaskEntity
        coEvery { syncManager.queueOperation(any(), any(), any(), any<Any>()) } just Runs

        val result = repository.moveTask("task-1", "column-2", 0)

        assertTrue(result is NetworkResult.Success)
        coVerify { taskDao.moveTask("task-1", "column-2", 0) }
    }
}
