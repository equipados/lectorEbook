package com.ebookreader.core.tts.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackWakeLockControllerTest {

    @Test
    fun acquiresWakeLockWhenPlaybackStarts() {
        val wakeLock = FakeCpuWakeLock()
        val controller = PlaybackWakeLockController(wakeLock, timeoutMs = 15_000L)

        controller.onPlaybackStateChanged(isPlaying = true)

        assertTrue(wakeLock.held)
        assertEquals(1, wakeLock.acquireCalls)
        assertEquals(15_000L, wakeLock.lastAcquireTimeoutMs)
    }

    @Test
    fun doesNotAcquireAgainIfWakeLockIsAlreadyHeld() {
        val wakeLock = FakeCpuWakeLock().apply { held = true }
        val controller = PlaybackWakeLockController(wakeLock)

        controller.onPlaybackStateChanged(isPlaying = true)

        assertEquals(0, wakeLock.acquireCalls)
    }

    @Test
    fun releasesWakeLockWhenPlaybackStops() {
        val wakeLock = FakeCpuWakeLock().apply { held = true }
        val controller = PlaybackWakeLockController(wakeLock)

        controller.onPlaybackStateChanged(isPlaying = false)

        assertFalse(wakeLock.held)
        assertEquals(1, wakeLock.releaseCalls)
    }

    @Test
    fun releasesWakeLockOnServiceStop() {
        val wakeLock = FakeCpuWakeLock().apply { held = true }
        val controller = PlaybackWakeLockController(wakeLock)

        controller.onServiceStopping()

        assertFalse(wakeLock.held)
        assertEquals(1, wakeLock.releaseCalls)
    }

    private class FakeCpuWakeLock : CpuWakeLock {
        var held: Boolean = false
        var acquireCalls: Int = 0
        var releaseCalls: Int = 0
        var lastAcquireTimeoutMs: Long = -1L

        override fun acquire(timeoutMs: Long) {
            acquireCalls++
            held = true
            lastAcquireTimeoutMs = timeoutMs
        }

        override fun release() {
            releaseCalls++
            held = false
        }

        override fun isHeld(): Boolean = held
    }
}
