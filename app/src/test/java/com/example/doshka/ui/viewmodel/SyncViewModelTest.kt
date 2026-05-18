package com.example.doshka.ui.viewmodel

import app.cash.turbine.test
import com.example.doshka.data.network.NetworkMonitor
import com.example.doshka.data.network.NetworkStatus
import com.example.doshka.data.network.NetworkType
import com.example.doshka.data.sync.SyncManager
import com.example.doshka.data.sync.SyncState
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {

    @MockK
    private lateinit var syncManager: SyncManager

    @MockK
    private lateinit var networkMonitor: NetworkMonitor

    private lateinit var viewModel: SyncViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { syncManager.syncState } returns MutableStateFlow(SyncState.Idle)
        every { syncManager.pendingOperationsCount } returns MutableStateFlow(0)
        every { syncManager.getLastSyncTime() } returns flowOf(0L)
        every { networkMonitor.isOnline } returns MutableStateFlow(true)
        every { networkMonitor.networkStatus } returns flowOf(NetworkStatus.Available(NetworkType.WIFI))

        viewModel = SyncViewModel(syncManager, networkMonitor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `syncState exposes syncManager state`() = runTest {
        viewModel.syncState.test {
            assertEquals(SyncState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isOnline exposes networkMonitor state`() = runTest {
        viewModel.isOnline.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `triggerSync calls syncManager`() {
        every { syncManager.triggerSync() } just Runs

        viewModel.triggerSync()

        verify { syncManager.triggerSync() }
    }

    @Test
    fun `cancelPendingOperations calls syncManager`() = runTest {
        coEvery { syncManager.cancelAllPendingOperations() } just Runs

        viewModel.cancelPendingOperations()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { syncManager.cancelAllPendingOperations() }
    }
}

class SyncUiStateTest {

    @Test
    fun `showSyncBanner is true when syncing`() {
        val state = SyncUiState(syncState = SyncState.Syncing)
        assertTrue(state.showSyncBanner)
    }

    @Test
    fun `showSyncBanner is true when offline`() {
        val state = SyncUiState(syncState = SyncState.Offline)
        assertTrue(state.showSyncBanner)
    }

    @Test
    fun `showSyncBanner is true when error`() {
        val state = SyncUiState(syncState = SyncState.Error("error"))
        assertTrue(state.showSyncBanner)
    }

    @Test
    fun `showSyncBanner is true when pending operations exist`() {
        val state = SyncUiState(syncState = SyncState.Synced, pendingCount = 5)
        assertTrue(state.showSyncBanner)
    }

    @Test
    fun `showSyncBanner is false when synced and no pending`() {
        val state = SyncUiState(syncState = SyncState.Synced, pendingCount = 0)
        assertFalse(state.showSyncBanner)
    }

    @Test
    fun `statusMessage returns correct text for syncing`() {
        val state = SyncUiState(syncState = SyncState.Syncing)
        assertEquals("Синхронізація...", state.statusMessage)
    }

    @Test
    fun `statusMessage returns correct text for offline with pending`() {
        val state = SyncUiState(syncState = SyncState.Offline, pendingCount = 3)
        assertEquals("Офлайн · 3 змін", state.statusMessage)
    }

    @Test
    fun `statusMessage returns correct text for synced`() {
        val state = SyncUiState(syncState = SyncState.Synced, pendingCount = 0)
        assertEquals("Синхронізовано", state.statusMessage)
    }
}
