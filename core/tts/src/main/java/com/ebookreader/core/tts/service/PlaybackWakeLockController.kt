package com.ebookreader.core.tts.service

import android.os.PowerManager

internal interface CpuWakeLock {
    fun acquire(timeoutMs: Long)
    fun release()
    fun isHeld(): Boolean
}

internal class AndroidCpuWakeLock(
    private val wakeLock: PowerManager.WakeLock
) : CpuWakeLock {
    override fun acquire(timeoutMs: Long) {
        wakeLock.acquire(timeoutMs)
    }

    override fun release() {
        wakeLock.release()
    }

    override fun isHeld(): Boolean = wakeLock.isHeld
}

internal class PlaybackWakeLockController(
    private val wakeLock: CpuWakeLock,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS
) {
    fun onPlaybackStateChanged(isPlaying: Boolean) {
        if (isPlaying) {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(timeoutMs)
            }
            return
        }

        if (wakeLock.isHeld()) {
            wakeLock.release()
        }
    }

    fun onServiceStopping() {
        if (wakeLock.isHeld()) {
            wakeLock.release()
        }
    }

    companion object {
        // Safety timeout in case service callbacks are interrupted unexpectedly.
        private const val DEFAULT_TIMEOUT_MS: Long = 60L * 60L * 1000L
    }
}
