package com.example.doshka.data.network

import org.junit.Assert.*
import org.junit.Test

class NetworkMonitorTest {

    @Test
    fun `NetworkType enum has correct values`() {
        assertEquals(5, NetworkType.entries.size)
        assertTrue(NetworkType.entries.contains(NetworkType.WIFI))
        assertTrue(NetworkType.entries.contains(NetworkType.CELLULAR))
        assertTrue(NetworkType.entries.contains(NetworkType.ETHERNET))
        assertTrue(NetworkType.entries.contains(NetworkType.OTHER))
        assertTrue(NetworkType.entries.contains(NetworkType.NONE))
    }

    @Test
    fun `NetworkStatus Available contains type`() {
        val status = NetworkStatus.Available(NetworkType.WIFI)
        assertEquals(NetworkType.WIFI, status.type)
    }

    @Test
    fun `NetworkStatus Lost is singleton`() {
        val lost1 = NetworkStatus.Lost
        val lost2 = NetworkStatus.Lost
        assertSame(lost1, lost2)
    }

    @Test
    fun `NetworkStatus Unavailable is singleton`() {
        val unavailable1 = NetworkStatus.Unavailable
        val unavailable2 = NetworkStatus.Unavailable
        assertSame(unavailable1, unavailable2)
    }

    @Test
    fun `NetworkStatus types are distinguishable`() {
        val available = NetworkStatus.Available(NetworkType.WIFI)
        val lost = NetworkStatus.Lost
        val unavailable = NetworkStatus.Unavailable

        assertNotEquals(available, lost)
        assertNotEquals(available, unavailable)
        assertNotEquals(lost, unavailable)
    }
}
