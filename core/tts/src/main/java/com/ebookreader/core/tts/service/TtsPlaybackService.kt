package com.ebookreader.core.tts.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ebookreader.core.tts.controller.TtsController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TtsPlaybackService : Service() {

    @Inject
    lateinit var ttsController: TtsController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val CHANNEL_ID = "tts_playback"
        const val NOTIFICATION_ID = 1

        const val ACTION_PLAY_PAUSE = "com.ebookreader.ACTION_PLAY_PAUSE"
        const val ACTION_STOP = "com.ebookreader.ACTION_STOP"
        const val ACTION_NEXT = "com.ebookreader.ACTION_NEXT"
        const val ACTION_PREV = "com.ebookreader.ACTION_PREV"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeTtsState()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> serviceScope.launch {
                if (ttsController.state.value.isPlaying) {
                    ttsController.pause()
                } else {
                    ttsController.play()
                }
            }

            ACTION_STOP -> serviceScope.launch {
                ttsController.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_NEXT -> serviceScope.launch {
                ttsController.nextSentence()
            }

            ACTION_PREV -> serviceScope.launch {
                ttsController.previousSentence()
            }
        }

        val initialState = ttsController.state.value
        startForeground(NOTIFICATION_ID, buildNotification(initialState.isPlaying))

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "TTS Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controls for text-to-speech playback"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun observeTtsState() {
        serviceScope.launch {
            ttsController.state.collectLatest { state ->
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, buildNotification(state.isPlaying))
            }
        }
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val previousIntent = PendingIntent.getService(
            this,
            10,
            Intent(this, TtsPlaybackService::class.java).apply {
                action = ACTION_PREV
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this,
            11,
            Intent(this, TtsPlaybackService::class.java).apply {
                action = ACTION_PLAY_PAUSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this,
            12,
            Intent(this, TtsPlaybackService::class.java).apply {
                action = ACTION_NEXT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            13,
            Intent(this, TtsPlaybackService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val playPauseTitle = if (isPlaying) "Pause" else "Play"
        val statusText = if (isPlaying) "Playing" else "Paused"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Ebook Reader")
            .setContentText(statusText)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", previousIntent)
            .addAction(playPauseIcon, playPauseTitle, playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }
}
