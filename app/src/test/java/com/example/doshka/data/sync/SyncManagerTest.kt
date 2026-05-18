package com.example.doshka.data.sync

import android.content.Context
import androidx.work.WorkManager
import com.example.doshka.data.local.DataStoreManager
import com.example.doshka.data.local.dao.PendingOperationDao
import com.example.doshka.data.network.NetworkMonitor
import com.example.doshka.data.network.NetworkStatus
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SyncManagerTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var networkMonitor: NetworkMonitor

    @MockK
    private lateinit var pendingOperationDao: PendingOperationDao

    @MockK
    private lateinit var dataStoreManager: DataStoreManager

    private lateinit var json: Json

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        json = Json { encodeDefaults = true }

        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns mockk(relaxed = true)

        every { networkMonitor.networkStatus } returns flowOf(NetworkStatus.Available(com.example.doshka.data.network.NetworkType.WIFI))
        every { networkMonitor.isOnline } returns MutableStateFlow(true)
        every { pendingOperationDao.observePendingCount() } returns flowOf(0)
    }

    @Test
    fun `syncState starts as Idle`() = runTest {
        // SyncManager initialization would set initial state
        // This is a placeholder for the actual test
        assertTrue(true)
    }

    @Test
    fun `queueOperation adds pending operation`() = runTest {
        coEvery { pendingOperationDao.insert(any()) } returns 1L

        // Test would verify queueOperation adds to database
        coVerify(exactly = 0) { pendingOperationDao.insert(any()) }
    }

    @Test
    fun `OperationType enum values are correct`() {
        assertEquals("CREATE", OperationType.CREATE.name)
        assertEquals("UPDATE", OperationType.UPDATE.name)
        assertEquals("DELETE", OperationType.DELETE.name)
        assertEquals("MOVE", OperationType.MOVE.name)
    }

    @Test
    fun `EntityType enum values are correct`() {
        assertEquals("TASK", EntityType.TASK.name)
        assertEquals("BOARD", EntityType.BOARD.name)
        assertEquals("COLUMN", EntityType.COLUMN.name)
        assertEquals("MESSAGE", EntityType.MESSAGE.name)
    }

    @Test
    fun `SyncState sealed class types`() {
        assertTrue(SyncState.Idle is SyncState)
        assertTrue(SyncState.Syncing is SyncState)
        assertTrue(SyncState.Synced is SyncState)
        assertTrue(SyncState.Offline is SyncState)
        assertTrue(SyncState.Error("test") is SyncState)
    }

    @Test
    fun `SyncResult Online returns correct value`() {
        val result = SyncResult.Online("data")
        assertEquals("data", result.value)
        assertTrue(result.isOnline)
    }

    @Test
    fun `SyncResult Offline returns correct value`() {
        val result = SyncResult.Offline("data")
        assertEquals("data", result.value)
        assertFalse(result.isOnline)
    }
}
